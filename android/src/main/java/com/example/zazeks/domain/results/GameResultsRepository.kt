package com.example.zazeks.domain.results

interface GameResultsRepository {
    suspend fun fetchUserRounds(userId: Int): List<RoundResult>
}
