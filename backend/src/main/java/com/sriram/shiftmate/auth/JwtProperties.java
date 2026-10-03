package com.sriram.shiftmate.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration ttl, String issuer) {
    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 characters");
        }
        if (ttl == null) {
            ttl = Duration.ofHours(12);
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "shiftmate";
        }
    }
}
