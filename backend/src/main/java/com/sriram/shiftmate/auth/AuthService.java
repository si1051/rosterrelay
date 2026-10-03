package com.sriram.shiftmate.auth;

import com.sriram.shiftmate.account.Account;
import com.sriram.shiftmate.account.AccountRepository;
import com.sriram.shiftmate.auth.AuthDtos.LoginRequest;
import com.sriram.shiftmate.auth.AuthDtos.RegisterRequest;
import com.sriram.shiftmate.auth.AuthDtos.TokenResponse;
import com.sriram.shiftmate.common.ConflictException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final AccountRepository accounts;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LoginRateLimiter rateLimiter;

    public AuthService(AccountRepository accounts, PasswordEncoder encoder, JwtService jwt,
                       LoginRateLimiter rateLimiter) {
        this.accounts = accounts;
        this.encoder = encoder;
        this.jwt = jwt;
        this.rateLimiter = rateLimiter;
    }

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (accounts.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        Account a = new Account(email, encoder.encode(req.password()), req.name().trim(), req.role());
        a.setOrganization(req.organization() == null ? null : req.organization().trim());
        a.setPhone(req.phone());
        return token(accounts.save(a));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        String email = req.email().trim();
        rateLimiter.checkAllowed(email);
        Account a = accounts.findByEmailIgnoreCase(email)
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElse(null);
        if (a == null) {
            rateLimiter.recordFailure(email);
            throw new BadCredentialsException("Invalid credentials");
        }
        rateLimiter.recordSuccess(email);
        return token(a);
    }

    private TokenResponse token(Account a) {
        return new TokenResponse(jwt.issue(a.getId(), a.getEmail(), a.getRole().name()), "Bearer",
                jwt.ttlSeconds(), a.getId(), a.getName(), a.getRole());
    }
}
