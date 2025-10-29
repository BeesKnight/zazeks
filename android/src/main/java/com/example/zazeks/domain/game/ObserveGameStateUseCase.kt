package com.example.zazeks.domain.game

import kotlinx.coroutines.flow.Flow

/**
 * Use-case that exposes a reactive stream with game updates.
 */
class ObserveGameStateUseCase(
    private val engine: GameEngine
) {
    operator fun invoke(): Flow<GameResult<GameSnapshot>> = engine.observeGame()
}

/**
 * Contract for the game engine responsible for calculating states and emitting updates.
 */
interface GameEngine {
    fun observeGame(): Flow<GameResult<GameSnapshot>>
}
