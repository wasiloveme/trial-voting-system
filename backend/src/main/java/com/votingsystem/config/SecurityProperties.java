package com.votingsystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        String aesSecretKey,
        String jwtSecret,
        long jwtExpirationMs,
        List<String> adminIpWhitelist
) {
}
