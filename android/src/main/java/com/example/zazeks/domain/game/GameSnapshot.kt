package com.example.zazeks.domain.game

/**
 * Immutable snapshot of the current game session that the presentation layer renders.
 */
data class GameSnapshot(
    val sessionId: String,
    val board: List<String>,
    val currentPlayer: String,
    val turn: Int,
    val isCompleted: Boolean
)
