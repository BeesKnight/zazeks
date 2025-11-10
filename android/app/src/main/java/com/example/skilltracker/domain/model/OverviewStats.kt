package com.example.skilltracker.domain.model

data class OverviewStats(
    val totalMinutes: Long,
    val skillsCount: Long,
    val sessionsCount: Long,
    val bySkill: List<OverviewSkillBreakdown>,
    val inactiveSkills: List<InactiveSkill>
)
