package com.example.skilltracker.data.dto

data class OverviewStatsDto(
    val totalMinutes: Long,
    val skillsCount: Long,
    val sessionsCount: Long,
    val bySkill: List<OverviewSkillBreakdownDto>,
    val inactiveSkills: List<InactiveSkillDto>
)
