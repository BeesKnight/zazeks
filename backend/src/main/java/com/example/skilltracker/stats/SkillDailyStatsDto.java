package com.example.skilltracker.stats;

import java.time.LocalDate;

public record SkillDailyStatsDto(LocalDate date, long minutes) {
}
