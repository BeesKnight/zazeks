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
    suspend fun startNewGame(): GameResult<GameSnapshot>
    suspend fun resumeLastGame(): GameResult<GameSnapshot>
    suspend fun playMove(row: Int, column: Int): GameResult<GameSnapshot>
    suspend fun abandonGame()
}
