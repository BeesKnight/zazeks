package com.example.zazeks.domain.game

/**
 * Repository responsible for persisting and restoring the progress of the current and completed games.
 */
interface GameStateRepository {
    suspend fun saveActive(snapshot: GameSnapshot)
    suspend fun loadActive(): GameSnapshot?
    suspend fun clearActive()
    suspend fun saveCompleted(snapshot: GameSnapshot)
    suspend fun loadLastCompleted(): GameSnapshot?
}
