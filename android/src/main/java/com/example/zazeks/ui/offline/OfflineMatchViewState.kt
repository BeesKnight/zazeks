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
    val errorMessageRes: Int? = null,
    val boundingBox: BoundingBoxUiModel? = null,
    val confidence: Double? = null,
) : Parcelable {
    fun hasGesture(): Boolean = isRecognizedGesture(gesture)
    fun hasBoundingBox(): Boolean = boundingBox?.isValid() == true
}

@Parcelize
data class BoundingBoxUiModel(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val label: String? = null,
    val confidence: Double? = null,
) : Parcelable {
    fun isValid(): Boolean = right > left && bottom > top

    fun clamp(): BoundingBoxUiModel = BoundingBoxUiModel(
        left = left.coerceIn(0f, 1f),
        top = top.coerceIn(0f, 1f),
        right = right.coerceIn(0f, 1f),
        bottom = bottom.coerceIn(0f, 1f),
        label = label,
        confidence = confidence,
    )
}
