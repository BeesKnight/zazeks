package com.example.zazeks.domain.game

import javax.inject.Inject

class GetActiveGameSnapshotUseCase @Inject constructor(
    private val repository: GameStateRepository
) {
    suspend operator fun invoke(): GameSnapshot? = repository.loadActive()
}
