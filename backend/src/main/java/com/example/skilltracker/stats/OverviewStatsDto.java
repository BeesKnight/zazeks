package com.example.skilltracker.stats;

import java.util.List;

public record OverviewStatsDto(long totalMinutes,
                               long skillsCount,
                               long sessionsCount,
                               List<OverviewSkillBreakdownDto> bySkill,
                               List<InactiveSkillDto> inactiveSkills) {
}
