package com.example.zazeks.domain.results

import java.time.Instant

data class RoundResult(
    val id: String,
    val mode: ResultMode,
    val playerGesture: String?,
    val opponentGesture: String?,
    val result: String?,
    val timestamp: Instant?,
    val playerScore: Int? = null,
    val opponentScore: Int? = null,
)
