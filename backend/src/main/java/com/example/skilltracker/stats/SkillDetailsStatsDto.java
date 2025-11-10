package com.example.skilltracker.stats;

import java.util.List;

public record SkillDetailsStatsDto(Long skillId,
                                   String skillName,
                                   long totalMinutes,
                                   long sessionsCount,
                                   Double averageDifficulty,
                                   List<SkillDailyStatsDto> byDay) {
}
