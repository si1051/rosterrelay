package com.sriram.rosterrelay.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    /** Authenticated identity carried by a valid token. */
    public record Principal(Long accountId, String role) {
    }

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issue(Long accountId, String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(String.valueOf(accountId))
                .claim("email", email)
                .claim("role", role)
                .setIssuer(props.issuer())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(props.ttl())))
                .signWith(key)
                .compact();
    }

    public Optional<Principal> verify(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .requireIssuer(props.issuer())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Optional.of(new Principal(Long.parseLong(claims.getSubject()), claims.get("role", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long ttlSeconds() {
        return props.ttl().toSeconds();
    }
}
