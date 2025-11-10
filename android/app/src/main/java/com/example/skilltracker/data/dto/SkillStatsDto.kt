package com.example.skilltracker.data.dto

data class SkillStatsDto(
    val skillId: Long,
    val skillName: String,
    val sessionCount: Long,
    val totalDurationMinutes: Long
)
