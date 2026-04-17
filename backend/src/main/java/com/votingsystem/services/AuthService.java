package com.votingsystem.services;

import com.votingsystem.dto.*;
import com.votingsystem.models.User;
import com.votingsystem.models.VoterWhitelist;
import com.votingsystem.repositories.UserRepository;
import com.votingsystem.repositories.VoterWhitelistRepository;
import com.votingsystem.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final VoterWhitelistRepository whitelistRepository;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;
    private final AuditLogService auditLogService;

    public AuthService(UserRepository userRepository, 
                       VoterWhitelistRepository whitelistRepository, 
                       JwtUtils jwtUtils, 
                       PasswordEncoder passwordEncoder, 
                       AuthenticationManager authenticationManager, 
                       OtpService otpService, 
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.whitelistRepository = whitelistRepository;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.otpService = otpService;
        this.auditLogService = auditLogService;
    }

    public AuthResponseDto login(String studentId, String password, String clientIp) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(studentId, password)
        );

        User user = userRepository.findByStudentId(studentId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getAccountStatus() == User.AccountStatus.LOCKED) {
            throw new RuntimeException("Account is locked.");
        }
        if (user.getAccountStatus() == User.AccountStatus.PENDING) {
            throw new RuntimeException("Account registration not yet verified via OTP.");
        }

        auditLogService.log(
            user.getId(), user.getStudentId(), user.getRole(),
            "USER_LOGIN", "User", user.getId(),
            "Successful login", clientIp
        );

        String token = jwtUtils.generateToken(user);
        
        AuthResponseDto response = new AuthResponseDto();
        response.setToken(token);
        response.setStudentId(user.getStudentId());
        response.setRole(user.getRole().name());
        return response;
    }

    public boolean checkWhitelist(String studentId) {
        return whitelistRepository.findByStudentId(studentId)
            .map(entry -> !entry.isRegistered())
            .orElse(false);
    }

    @Transactional
    public void beginRegistration(RegisterRequestDto request, String clientIp) {
        VoterWhitelist whitelistEntry = whitelistRepository.findByStudentId(request.getStudentId())
            .orElseThrow(() -> new RuntimeException("Student ID not found in whitelist. Access Denied."));
            
        if (whitelistEntry.isRegistered()) {
            throw new RuntimeException("Student is already registered.");
        }
        
        if (userRepository.existsByStudentId(request.getStudentId())) {
            throw new RuntimeException("Account already exists.");
        }

        User user = new User();
        user.setStudentId(request.getStudentId());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.USER);
        user.setAccountStatus(User.AccountStatus.PENDING);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setBirthday(request.getBirthday());
        user.setGmail(request.getGmail());

        userRepository.save(user);

        otpService.generateAndSendOtp(user.getGmail());
    }

    @Transactional
    public void confirmOtp(String studentId, String otpCode, String clientIp) {
        User user = userRepository.findByStudentId(studentId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getAccountStatus() != User.AccountStatus.PENDING) {
            throw new RuntimeException("Account is not in PENDING state.");
        }

        boolean isValid = otpService.validateOtp(user.getGmail(), otpCode);
        if (!isValid) throw new RuntimeException("Invalid or Expired OTP.");

        user.setAccountStatus(User.AccountStatus.ACTIVE);
        userRepository.save(user);

        VoterWhitelist whitelistEntry = whitelistRepository.findByStudentId(studentId)
            .orElseThrow(() -> new RuntimeException("Whitelist entry not found."));
        whitelistEntry.setRegistered(true);
        whitelistRepository.save(whitelistEntry);

        auditLogService.log(
            user.getId(), user.getStudentId(), user.getRole(),
            "ACCOUNT_ACTIVATED", "User", user.getId(),
            "OTP confirmed and account activated", clientIp
        );
    }

    @Transactional
    public void requestPasswordReset(String emailOrPhone) {
        User user = userRepository.findByGmail(emailOrPhone)
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        otpService.generateAndSendOtp(user.getGmail());
    }

    @Transactional
    public void resetPassword(String studentId, String otpCode, String newPassword, String confirmPassword, String clientIp) {
        User user = userRepository.findByStudentId(studentId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isValid = otpService.validateOtp(user.getGmail(), otpCode);
        if (!isValid) throw new RuntimeException("Invalid or Expired OTP.");

        if (!newPassword.equals(confirmPassword)) {
             throw new RuntimeException("Passwords do not match.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        auditLogService.log(
            user.getId(), user.getStudentId(), user.getRole(),
            "PASSWORD_RESET", "User", user.getId(),
            "Password reset successfully", clientIp
        );
    }
}
