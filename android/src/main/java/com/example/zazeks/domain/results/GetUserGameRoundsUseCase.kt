package com.example.zazeks.domain.results

import javax.inject.Inject

class GetUserGameRoundsUseCase @Inject constructor(
    private val repository: GameResultsRepository,
) {
    suspend operator fun invoke(userId: Int): List<RoundResult> = repository.fetchUserRounds(userId)
}
