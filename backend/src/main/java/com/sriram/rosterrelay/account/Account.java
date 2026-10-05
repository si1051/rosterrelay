package com.sriram.rosterrelay.account;

import jakarta.persistence.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

@Entity
@Table(name = "accounts", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "calendarToken")
})
public class Account {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Role role;

    /** Coordinators: the organization they run shifts for. */
    @Column(length = 120)
    private String organization;

    @Column(length = 30)
    private String phone;

    /** Secret for the read-only calendar feed URL (rotatable). */
    @Column(nullable = false, length = 32)
    private String calendarToken = newToken();

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Account() {
    }

    public Account(String email, String passwordHash, String name, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role;
    }

    public static String newToken() {
        byte[] b = new byte[16];
        RANDOM.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    public void rotateCalendarToken() {
        this.calendarToken = newToken();
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Role getRole() { return role; }
    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCalendarToken() { return calendarToken; }
    public Instant getCreatedAt() { return createdAt; }
}
