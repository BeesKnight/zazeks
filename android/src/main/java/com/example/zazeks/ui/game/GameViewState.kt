package com.example.zazeks.ui.game

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Representation of the game screen state that the UI observes via LiveData.
 */
sealed class ViewState {
    object Loading : ViewState()
    data class Content(val session: GameUiModel) : ViewState()
    data class Error(
        val title: String,
        val message: String,
        val isRecoverable: Boolean,
        val action: Action? = null
    ) : ViewState() {
        data class Action(val label: String, val analyticsTag: String)
    }
}

/**
 * UI friendly model of the game session, already formatted for rendering.
 */
@Parcelize
data class GameUiModel(
    val sessionId: String,
    val round: Int,
    val playerGesture: String?,
    val opponentGesture: String?,
    val remainingSeconds: Double,
    val playerScore: Int,
    val opponentScore: Int,
    val roundResult: String?,
    val matchResult: String?,
    val isRoundCompleted: Boolean,
    val isMatchCompleted: Boolean
) : Parcelable
