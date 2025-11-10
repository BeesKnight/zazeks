package com.example.skilltracker.session;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record UpdateSessionRequest(@NotNull(message = "Skill id is required") Long skillId,
                                   Instant sessionDate,
                                   @Min(value = 1, message = "Duration should be at least 1") int durationMinutes,
                                   String notes,
                                   Integer difficulty,
                                   String source) {
}
