package com.sriram.shiftmate.shift;

import jakarta.validation.constraints.*;

import java.time.Instant;

public final class ShiftDtos {

    private ShiftDtos() {
    }

    public record ShiftRequest(
            @NotBlank @Size(max = 120) String title,
            @Size(max = 2000) String description,
            @NotBlank @Size(max = 200) String location,
            @NotNull @Future Instant startsAt,
            @NotNull Instant endsAt,
            @Min(1) @Max(500) int capacity) {

        @AssertTrue(message = "a shift must end after it starts and last at most 12 hours")
        public boolean isDurationValid() {
            return startsAt == null || endsAt == null
                    || (endsAt.isAfter(startsAt) && !endsAt.isAfter(startsAt.plusSeconds(12 * 3600)));
        }
    }
}
