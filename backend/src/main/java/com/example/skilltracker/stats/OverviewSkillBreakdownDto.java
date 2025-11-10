package com.example.skilltracker.stats;

public record OverviewSkillBreakdownDto(Long skillId,
                                        String skillName,
                                        long minutes,
                                        long sessions) {
}
