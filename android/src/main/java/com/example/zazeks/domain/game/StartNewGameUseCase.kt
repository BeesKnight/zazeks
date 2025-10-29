package com.example.zazeks.domain.game

import javax.inject.Inject

class StartNewGameUseCase @Inject constructor(
    private val engine: GameEngine
) {
    suspend operator fun invoke(): GameResult<GameSnapshot> = engine.startNewGame()
}
