package com.example.skilltracker.domain.model

data class SkillStats(
    val skillId: Long,
    val skillName: String,
    val sessionCount: Long,
    val totalDurationMinutes: Long
)
