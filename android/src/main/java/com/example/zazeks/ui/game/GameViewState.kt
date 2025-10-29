package com.example.zazeks.ui.game

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
data class GameUiModel(
    val sessionId: String,
    val boardRows: List<String>,
    val currentPlayerLabel: String,
    val turnLabel: String,
    val completionLabel: String?
)
