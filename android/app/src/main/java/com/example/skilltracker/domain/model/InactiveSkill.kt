package com.example.skilltracker.domain.model

data class InactiveSkill(
    val skillId: Long,
    val skillName: String,
    val daysSinceLastSession: Long
)
