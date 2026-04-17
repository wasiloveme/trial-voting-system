package com.votingsystem.security;

import com.votingsystem.models.User;
import com.votingsystem.repositories.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Custom UserDetailsService implementation for Spring Security.
 * Maps our domain User to Spring Security's UserDetails.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByStudentId(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with Student ID: " + username));

        // Use the student ID as username and the hashed password.
        // Grant authority based on the user's role prefixing with "ROLE_".
        return new org.springframework.security.core.userdetails.User(
            user.getStudentId(),
            user.getPasswordHash(),
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
