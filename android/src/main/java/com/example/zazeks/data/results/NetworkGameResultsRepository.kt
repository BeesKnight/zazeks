package com.example.zazeks.data.results

import com.example.zazeks.domain.results.GameResultsRepository
import com.example.zazeks.domain.results.ResultMode
import com.example.zazeks.domain.results.RoundResult
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkGameResultsRepository @Inject constructor(
    private val api: GameResultsApi,
) : GameResultsRepository {

    override suspend fun fetchUserRounds(userId: Int): List<RoundResult> =
        api.getUserGames(userId)
            .map { it.toDomain() }
            .sortedByDescending { it.timestamp ?: Instant.EPOCH }

    private fun GameResultDto.toDomain(): RoundResult {
        val parsedTimestamp = timestamp?.let { value ->
            runCatching { Instant.parse(value) }.getOrNull()
        }
        val identifier = id?.toString() ?: buildString {
            append(userId)
            append(":")
            append(userChoice ?: "?")
            append(":")
            append(timestamp ?: System.currentTimeMillis())
        }
        return RoundResult(
            id = identifier,
            mode = ResultMode.ONLINE,
            playerGesture = userChoice,
            opponentGesture = computerChoice,
            result = result,
            timestamp = parsedTimestamp,
            playerScore = null,
            opponentScore = null,
        )
    }
}
