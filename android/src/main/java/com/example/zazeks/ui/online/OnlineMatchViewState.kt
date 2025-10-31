package com.example.zazeks.ui.online

import com.example.zazeks.data.multiplayer.MultiplayerStats
import com.example.zazeks.ui.offline.DetectionUiModel

sealed class OnlineMatchViewState {
    object Loading : OnlineMatchViewState()
    data class Content(
        val session: OnlineMatchUiModel,
        val detection: DetectionUiModel,
    ) : OnlineMatchViewState()
    data class Error(val title: String, val message: String) : OnlineMatchViewState()
}

data class OnlineMatchUiModel(
    val sessionId: String? = null,
    val opponentName: String? = null,
    val round: Int = 1,
    val playerReady: Boolean = false,
    val opponentReady: Boolean = false,
    val remainingMillis: Long = 0L,
    val statusMessage: String? = null,
    val playerGesture: String? = null,
    val opponentGesture: String? = null,
    val playerScore: Int = 0,
    val opponentScore: Int = 0,
    val matchResult: String? = null,
    val blackout: Boolean = false,
    val showPlayAgain: Boolean = false,
    val stats: MultiplayerStats = MultiplayerStats(),
)

sealed class OnlineMatchEffect {
    data class NavigateToResults(val result: com.example.zazeks.ui.offline.GameResultArgs) : OnlineMatchEffect()
    data class ShowToast(val message: String) : OnlineMatchEffect()
}
