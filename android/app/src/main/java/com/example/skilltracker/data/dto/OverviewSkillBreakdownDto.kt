package com.example.skilltracker.data.dto

data class OverviewSkillBreakdownDto(
    val skillId: Long,
    val skillName: String,
    val minutes: Long,
    val sessions: Long
)
