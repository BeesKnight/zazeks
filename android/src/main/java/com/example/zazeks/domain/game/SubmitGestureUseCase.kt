package com.example.zazeks.domain.game

import javax.inject.Inject

class SubmitGestureUseCase @Inject constructor(
    private val engine: GameEngine
) {
    suspend operator fun invoke(gesture: String): GameResult<GameSnapshot> =
        engine.submitGesture(gesture)
}
