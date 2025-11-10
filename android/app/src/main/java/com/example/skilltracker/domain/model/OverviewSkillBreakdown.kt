package com.example.skilltracker.domain.model

data class OverviewSkillBreakdown(
    val skillId: Long,
    val skillName: String,
    val minutes: Long,
    val sessions: Long
)
