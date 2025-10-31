package com.example.zazeks.ui.game

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class GameEffect {
    data class NavigateToResults(val result: GameResultArgs) : GameEffect()
    object NavigateToMenu : GameEffect()
}

@Parcelize
data class GameResultArgs(
    val sessionId: String,
    val playerGesture: String?,
    val opponentGesture: String?,
    val playerScore: Int,
    val opponentScore: Int,
    val roundCount: Int,
    val matchResult: String?
) : Parcelable
