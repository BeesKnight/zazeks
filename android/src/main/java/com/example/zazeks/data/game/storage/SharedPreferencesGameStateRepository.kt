package com.example.zazeks.data.game.storage

import android.content.SharedPreferences
import com.example.zazeks.domain.game.GameSnapshot
import com.example.zazeks.domain.game.GameStateRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONArray
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
                null
            }
        }
    }

    private fun GameSnapshot.toJson(): String {
        val json = JSONObject()
        json.put(FIELD_SESSION_ID, sessionId)
        json.put(FIELD_CURRENT_PLAYER, currentPlayer)
        json.put(FIELD_TURN, turn)
        json.put(FIELD_COMPLETED, isCompleted)
        json.put(FIELD_WINNER, winner)
        val boardArray = JSONArray()
        board.forEach { boardArray.put(it) }
        json.put(FIELD_BOARD, boardArray)
        return json.toString()
    }

    private object GameSnapshotJsonAdapter {
        fun fromJson(json: String): GameSnapshot {
            val jsonObject = JSONObject(json)
            val boardArray = jsonObject.getJSONArray(FIELD_BOARD)
            val length = boardArray.length()
            val boardRows = MutableList(length) { index -> boardArray.getString(index) }
            val winner = if (jsonObject.isNull(FIELD_WINNER)) null else jsonObject.getString(FIELD_WINNER)
            return GameSnapshot(
                sessionId = jsonObject.getString(FIELD_SESSION_ID),
                board = boardRows,
                currentPlayer = jsonObject.getString(FIELD_CURRENT_PLAYER),
                turn = jsonObject.getInt(FIELD_TURN),
                isCompleted = jsonObject.getBoolean(FIELD_COMPLETED),
                winner = winner
            )
        }
    }

    companion object {
        private const val KEY_ACTIVE_GAME = "active_game"
        private const val KEY_LAST_COMPLETED_GAME = "last_completed_game"
        private const val FIELD_SESSION_ID = "sessionId"
        private const val FIELD_BOARD = "board"
        private const val FIELD_CURRENT_PLAYER = "currentPlayer"
        private const val FIELD_TURN = "turn"
        private const val FIELD_COMPLETED = "completed"
        private const val FIELD_WINNER = "winner"
    }
}
