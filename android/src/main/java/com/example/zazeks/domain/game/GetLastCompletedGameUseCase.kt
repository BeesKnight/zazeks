package com.example.zazeks.domain.game

import javax.inject.Inject

class GetLastCompletedGameUseCase @Inject constructor(
    private val repository: GameStateRepository
) {
    suspend operator fun invoke(): GameSnapshot? = repository.loadLastCompleted()
}
