package com.sriram.rosterrelay.auth;

import com.sriram.rosterrelay.common.NotFoundException;
import com.sriram.rosterrelay.account.Account;
import com.sriram.rosterrelay.account.AccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    private final AccountRepository accounts;

    public CurrentUser(AccountRepository accounts) {
        this.accounts = accounts;
    }

    public Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long id)) {
            throw new IllegalStateException("No authenticated user");
        }
        return id;
    }

    public Account get() {
        return accounts.findById(id()).orElseThrow(() -> new NotFoundException("Account not found"));
    }
}
