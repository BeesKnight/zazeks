package com.example.zazeks.ui.offline

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class OfflineMatchEffect {
    data class NavigateToResults(val result: GameResultArgs) : OfflineMatchEffect()
    object NavigateToMenu : OfflineMatchEffect()
}

@Parcelize
data class GameResultArgs(
    val sessionId: String,
    val playerGesture: String?,
    val opponentGesture: String?,
    val playerScore: Int,
    val opponentScore: Int,
    val roundCount: Int,
    val matchResult: String?,
    val mode: String? = null,
) : Parcelable
