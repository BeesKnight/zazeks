package com.example.skilltracker.data.dto

data class UpdateSessionRequest(
    val skillId: Long,
    val sessionDate: String?,
    val durationMinutes: Int,
    val notes: String?,
    val difficulty: Int?,
    val source: String?
)
