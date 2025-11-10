package com.example.skilltracker.data.dto

data class SkillDetailsStatsDto(
    val skillId: Long,
    val skillName: String,
    val totalMinutes: Long,
    val sessionsCount: Long,
    val averageDifficulty: Double?,
    val byDay: List<SkillDailyStatsDto>
)
