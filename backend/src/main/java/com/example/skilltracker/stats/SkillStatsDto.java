package com.example.skilltracker.stats;

public record SkillStatsDto(Long skillId, String skillName, long sessionCount, long totalDurationMinutes) {
}
