package com.example.zazeks.domain.game

/**
 * Represents errors that may happen while executing game logic.
 * Each subtype holds contextual information that can be surfaced by the UI layer.
 */
sealed class GameError {
    /**
     * A move is invalid, e.g. violates the game rules. The reason is ready for user facing text.
     */
    data class InvalidMove(val reason: String) : GameError()

    /**
     * The session is no longer valid (token expired, game finished, etc.).
     */
    data class SessionExpired(val sessionId: String? = null) : GameError()

    /**
     * Connectivity issues or timeouts when synchronising state with the backend.
     */
    data class Network(val cause: Throwable? = null) : GameError()

    /**
     * Data was corrupted or failed validation which means the session cannot continue safely.
     */
    data class DataCorrupted(val cause: Throwable? = null) : GameError()

    /**
     * Any other unexpected situation. The original cause is preserved for diagnostics.
     */
    data class Unknown(val cause: Throwable? = null) : GameError()
}

/**
 * Result wrapper around game operations so the UI can react to both data and errors.
 */
sealed class GameResult<out T> {
    data class Success<out T>(val value: T) : GameResult<T>()
    data class Failure(val error: GameError) : GameResult<Nothing>()

    inline fun <R> map(transform: (T) -> R): GameResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    inline fun onSuccess(action: (T) -> Unit): GameResult<T> = apply {
        if (this is Success) action(value)
    }

    inline fun onFailure(action: (GameError) -> Unit): GameResult<T> = apply {
        if (this is Failure) action(error)
    }

    companion object {
        fun <T> success(value: T): GameResult<T> = Success(value)
        fun failure(error: GameError): GameResult<Nothing> = Failure(error)
    }
}
