package com.example.zazeks.ui.offline

import android.os.Parcelable
import com.example.zazeks.core.gestures.isRecognizedGesture
import kotlinx.parcelize.Parcelize

/**
 * Representation of the offline match screen state that the UI observes via LiveData.
 */
sealed class ViewState {
    object Loading : ViewState()
    data class Content(
        val session: OfflineMatchUiModel,
        val detection: DetectionUiModel
    ) : ViewState()
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
data class OfflineMatchUiModel(
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

/**
 * Represents the most recent gesture detection snapshot produced by the camera pipeline.
 */
@Parcelize
data class DetectionUiModel(
    val gesture: String? = null,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val errorMessageRes: Int? = null
) : Parcelable {
    fun hasGesture(): Boolean = isRecognizedGesture(gesture)
}
