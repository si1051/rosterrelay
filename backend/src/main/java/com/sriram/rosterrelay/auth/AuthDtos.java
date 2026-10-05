package com.sriram.rosterrelay.auth;

import com.sriram.rosterrelay.account.Role;
import jakarta.validation.constraints.*;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72)
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "must contain letters and numbers")
            String password,
            @NotBlank @Size(max = 120) String name,
            @NotNull Role role,
            @Size(max = 120) String organization,
            @Size(max = 30) String phone) {

        @AssertTrue(message = "coordinators must name their organization")
        public boolean isOrganizationPresent() {
            return role != Role.COORDINATOR || (organization != null && !organization.isBlank());
        }
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record TokenResponse(String token, String tokenType, long expiresIn, Long accountId, String name,
                                Role role) {
    }
}
