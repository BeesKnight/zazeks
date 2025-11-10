package com.example.skilltracker.session;

import java.time.Instant;

public record SessionDto(Long id,
                         Long skillId,
                         Instant sessionDate,
                         int durationMinutes,
                         String notes,
                         Integer difficulty,
                         String source) {
}
