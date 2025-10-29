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
    val winner: String?,
    val turnCount: Int,
    val boardRows: List<String>
) : Parcelable
