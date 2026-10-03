package com.sriram.shiftmate.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByEmailIgnoreCase(String email);

    Optional<Account> findByCalendarToken(String token);

    boolean existsByEmailIgnoreCase(String email);
}
