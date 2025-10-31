package com.example.zazeks.domain.game

/**
 * Immutable snapshot of the current game session that the presentation layer renders.
 */
data class GameSnapshot(
    val sessionId: String,
    val round: Int,
    val playerGesture: String?,
    val opponentGesture: String?,
    val remainingMillis: Long,
    val playerScore: Int,
    val opponentScore: Int,
    val roundResult: String?,
    val matchResult: String?,
    val isRoundCompleted: Boolean,
    val isMatchCompleted: Boolean
)
