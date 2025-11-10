package com.example.skilltracker.data.dto

data class CreateSessionRequest(
    val skillId: Long,
    val sessionDate: String,
    val durationMinutes: Int,
    val notes: String?
)
