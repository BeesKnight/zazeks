package com.example.skilltracker.data.dto

data class SessionDto(
    val id: Long,
    val skillId: Long,
    val sessionDate: String,
    val durationMinutes: Int,
    val notes: String?
)
