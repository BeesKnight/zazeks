package com.example.zazeks.domain.game

import javax.inject.Inject

class AbandonGameUseCase @Inject constructor(
    private val engine: GameEngine
) {
    suspend operator fun invoke() {
        engine.abandonGame()
    }
}
