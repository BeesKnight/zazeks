package com.example.skilltracker.data.dto

data class InactiveSkillDto(
    val skillId: Long,
    val skillName: String,
    val daysSinceLastSession: Long
)
