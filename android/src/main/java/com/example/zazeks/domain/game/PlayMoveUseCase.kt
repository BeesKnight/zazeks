package com.example.zazeks.domain.game

import javax.inject.Inject

class PlayMoveUseCase @Inject constructor(
    private val engine: GameEngine
) {
    suspend operator fun invoke(row: Int, column: Int): GameResult<GameSnapshot> =
        engine.playMove(row, column)
}
