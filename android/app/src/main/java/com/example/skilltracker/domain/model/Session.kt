package com.example.skilltracker.domain.model

data class Session(
    val id: Long,
    val skillId: Long,
    val sessionDate: String,
    val durationMinutes: Int,
    val notes: String?,
    val difficulty: Int?,
    val source: String?
)
