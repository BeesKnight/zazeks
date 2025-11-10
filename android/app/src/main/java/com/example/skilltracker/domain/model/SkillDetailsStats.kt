package com.example.skilltracker.domain.model

data class SkillDetailsStats(
    val skillId: Long,
    val skillName: String,
    val totalMinutes: Long,
    val sessionsCount: Long,
    val averageDifficulty: Double?,
    val byDay: List<SkillDailyStats>
)
