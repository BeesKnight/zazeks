package com.example.zazeks.data.multiplayer

import android.content.SharedPreferences
import com.example.zazeks.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

interface MultiplayerStatsRepository {
    suspend fun load(): MultiplayerStats
    suspend fun recordResult(result: String?): MultiplayerStats
}

@Singleton
class SharedPreferencesMultiplayerStatsRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : MultiplayerStatsRepository {

    override suspend fun load(): MultiplayerStats = withContext(dispatcher) {
        MultiplayerStats(
            wins = sharedPreferences.getInt(KEY_WINS, 0),
            losses = sharedPreferences.getInt(KEY_LOSSES, 0),
            draws = sharedPreferences.getInt(KEY_DRAWS, 0),
        )
    }

    override suspend fun recordResult(result: String?): MultiplayerStats = withContext(dispatcher) {
        val current = load()
        val updated = current.increment(result)
        sharedPreferences.edit()
            .putInt(KEY_WINS, updated.wins)
            .putInt(KEY_LOSSES, updated.losses)
            .putInt(KEY_DRAWS, updated.draws)
            .apply()
        updated
    }

    companion object {
        private const val KEY_WINS = "multiplayer_wins"
        private const val KEY_LOSSES = "multiplayer_losses"
        private const val KEY_DRAWS = "multiplayer_draws"
    }
}

data class MultiplayerStats(
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
) {
    val total: Int get() = wins + losses + draws

    fun increment(result: String?): MultiplayerStats = when (result?.lowercase()) {
        "win" -> copy(wins = wins + 1)
        "loss" -> copy(losses = losses + 1)
        "draw" -> copy(draws = draws + 1)
        else -> this
    }
}
