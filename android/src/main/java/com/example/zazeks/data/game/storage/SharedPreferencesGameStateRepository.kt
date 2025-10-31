package com.example.zazeks.data.game.storage

import android.content.SharedPreferences
import com.example.zazeks.domain.game.GameSnapshot
import com.example.zazeks.domain.game.GameStateRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

@Singleton
class SharedPreferencesGameStateRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val ioDispatcher: CoroutineDispatcher
) : GameStateRepository {

    override suspend fun saveActive(snapshot: GameSnapshot) {
        persist(KEY_ACTIVE_GAME, snapshot)
    }

    override suspend fun loadActive(): GameSnapshot? = load(KEY_ACTIVE_GAME)

    override suspend fun clearActive() {
        withContext(ioDispatcher) {
            sharedPreferences.edit().remove(KEY_ACTIVE_GAME).apply()
        }
    }

    override suspend fun saveCompleted(snapshot: GameSnapshot) {
        persist(KEY_LAST_COMPLETED_GAME, snapshot)
    }

    override suspend fun loadLastCompleted(): GameSnapshot? = load(KEY_LAST_COMPLETED_GAME)

    private suspend fun persist(key: String, snapshot: GameSnapshot) {
        withContext(ioDispatcher) {
            sharedPreferences.edit().putString(key, snapshot.toJson()).apply()
        }
    }

    private suspend fun load(key: String): GameSnapshot? = withContext(ioDispatcher) {
        sharedPreferences.getString(key, null)?.let { value ->
            try {
                GameSnapshotJsonAdapter.fromJson(value)
            } catch (exception: JSONException) {
                sharedPreferences.edit().remove(key).apply()
                null
            }
        }
    }

    private fun GameSnapshot.toJson(): String {
        val json = JSONObject()
        json.put(FIELD_SESSION_ID, sessionId)
        json.put(FIELD_ROUND, round)
        json.put(FIELD_PLAYER_GESTURE, playerGesture)
        json.put(FIELD_OPPONENT_GESTURE, opponentGesture)
        json.put(FIELD_REMAINING_MILLIS, remainingMillis)
        json.put(FIELD_PLAYER_SCORE, playerScore)
        json.put(FIELD_OPPONENT_SCORE, opponentScore)
        json.put(FIELD_ROUND_RESULT, roundResult)
        json.put(FIELD_MATCH_RESULT, matchResult)
        json.put(FIELD_ROUND_COMPLETED, isRoundCompleted)
        json.put(FIELD_MATCH_COMPLETED, isMatchCompleted)
        return json.toString()
    }

    private object GameSnapshotJsonAdapter {
        fun fromJson(json: String): GameSnapshot {
            val jsonObject = JSONObject(json)
            if (jsonObject.has(FIELD_BOARD)) {
                throw JSONException("Legacy snapshot schema detected")
            }
            return GameSnapshot(
                sessionId = jsonObject.getString(FIELD_SESSION_ID),
                round = jsonObject.getInt(FIELD_ROUND),
                playerGesture = if (jsonObject.isNull(FIELD_PLAYER_GESTURE)) null else jsonObject.getString(FIELD_PLAYER_GESTURE),
                opponentGesture = if (jsonObject.isNull(FIELD_OPPONENT_GESTURE)) null else jsonObject.getString(FIELD_OPPONENT_GESTURE),
                remainingMillis = jsonObject.optLong(FIELD_REMAINING_MILLIS, 0L),
                playerScore = jsonObject.optInt(FIELD_PLAYER_SCORE, 0),
                opponentScore = jsonObject.optInt(FIELD_OPPONENT_SCORE, 0),
                roundResult = if (jsonObject.isNull(FIELD_ROUND_RESULT)) null else jsonObject.getString(FIELD_ROUND_RESULT),
                matchResult = if (jsonObject.isNull(FIELD_MATCH_RESULT)) null else jsonObject.getString(FIELD_MATCH_RESULT),
                isRoundCompleted = jsonObject.optBoolean(FIELD_ROUND_COMPLETED, false),
                isMatchCompleted = jsonObject.optBoolean(FIELD_MATCH_COMPLETED, false)
            )
        }
    }

    companion object {
        private const val KEY_ACTIVE_GAME = "active_game"
        private const val KEY_LAST_COMPLETED_GAME = "last_completed_game"
        private const val FIELD_SESSION_ID = "sessionId"
        private const val FIELD_ROUND = "round"
        private const val FIELD_PLAYER_GESTURE = "playerGesture"
        private const val FIELD_OPPONENT_GESTURE = "opponentGesture"
        private const val FIELD_REMAINING_MILLIS = "remainingMillis"
        private const val FIELD_PLAYER_SCORE = "playerScore"
        private const val FIELD_OPPONENT_SCORE = "opponentScore"
        private const val FIELD_ROUND_RESULT = "roundResult"
        private const val FIELD_MATCH_RESULT = "matchResult"
        private const val FIELD_ROUND_COMPLETED = "roundCompleted"
        private const val FIELD_MATCH_COMPLETED = "matchCompleted"
        private const val FIELD_BOARD = "board"
    }
}
