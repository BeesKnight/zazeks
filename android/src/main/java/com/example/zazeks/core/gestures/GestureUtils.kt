package com.example.zazeks.core.gestures

import android.content.Context
import com.example.zazeks.R
import java.util.Locale

private val VALID_GESTURES = setOf("rock", "paper", "scissors")
private val EMPTY_TOKENS = setOf("", "none", "unknown", "null")

/**
 * Normalizes gesture labels coming from the ML pipeline or the backend.
 */
fun normalizeGesture(raw: String?): String? {
    val normalized = raw?.trim()?.lowercase(Locale.ROOT) ?: return null
    return when {
        normalized in VALID_GESTURES -> normalized
        normalized in EMPTY_TOKENS -> null
        else -> null
    }
}

fun isRecognizedGesture(raw: String?): Boolean = normalizeGesture(raw) != null

fun Context.formatGesture(raw: String?): String = when (normalizeGesture(raw)) {
    "rock" -> getString(R.string.game_select_rock)
    "paper" -> getString(R.string.game_select_paper)
    "scissors" -> getString(R.string.game_select_scissors)
    else -> getString(R.string.game_gesture_unknown)
}
