package com.votingsystem.services;

import com.votingsystem.models.User;
import com.votingsystem.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int OTP_BOUND = 1_000_000;
    private static final String OTP_FROM_ADDRESS = "noreply@securedsupersystem.edu.ph";

    @Transactional
    public void generateAndSendOtp(String gmail) {
        User user = userRepository.findByGmail(gmail)
            .orElseThrow(() -> new RuntimeException("User not found with provided email"));

        String otpCode = String.format("%06d", new SecureRandom().nextInt(OTP_BOUND));
        user.setOtpCode(otpCode);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        userRepository.save(user);

        sendOtpEmail(user.getGmail(), otpCode);
    }

    @Transactional
    public boolean validateOtp(String gmail, String submittedCode) {
        User user = userRepository.findByGmail(gmail)
            .orElseThrow(() -> new RuntimeException("User not found with provided email"));

        if (user.getOtpCode() == null || user.getOtpExpiry() == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            return false;
        }

        boolean isValid = user.getOtpCode().equals(submittedCode);
        if (isValid) {
            user.setOtpCode(null);
            user.setOtpExpiry(null);
            userRepository.save(user);
        }

        return isValid;
    }

    private void sendOtpEmail(String toEmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(OTP_FROM_ADDRESS);
        message.setTo(toEmail);
        message.setSubject("Secured Super System - Your Verification Code");
        message.setText(String.format("""
                Hello,

                Your one-time verification code is: %s

                This code expires in %d minutes.
                Do not share this code with anyone.

                If you did not request this code, please contact the SSC immediately.

                - Secured Super System
                """, otpCode, OTP_EXPIRY_MINUTES));
        mailSender.send(message);
    }
}
