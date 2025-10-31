package com.example.zazeks.data.game

import com.example.zazeks.domain.game.GameEngine
import com.example.zazeks.domain.game.GameError
import com.example.zazeks.domain.game.GameResult
import com.example.zazeks.domain.game.GameSnapshot
import com.example.zazeks.domain.game.GameStateRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class DefaultGameEngine @Inject constructor(
    private val repository: GameStateRepository,
    private val ioDispatcher: CoroutineDispatcher
) : GameEngine {

    private val stateFlow = MutableSharedFlow<GameResult<GameSnapshot>>(replay = 1)
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private var state: EngineState? = null
    private var timerJob: Job? = null
    private var lastCompletedSignature: String? = null
    private val random = Random(System.currentTimeMillis())

    override fun observeGame(): Flow<GameResult<GameSnapshot>> = stateFlow.asSharedFlow()

    override suspend fun startNewGame(): GameResult<GameSnapshot> = withContext(ioDispatcher) {
        mutex.withLock {
            cancelTimerLocked()
            repository.clearActive()
            val freshState = EngineState.new()
            state = freshState
            lastCompletedSignature = null
            val result = emitStateLocked(freshState)
            startTimerLockedIfNeeded()
            result
        }
    }

    override suspend fun resumeLastGame(): GameResult<GameSnapshot> = withContext(ioDispatcher) {
        mutex.withLock {
            cancelTimerLocked()
            val savedSnapshot = repository.loadActive()
                ?: return@withLock emitFailure(GameError.SessionExpired())
            val restoredState = EngineState.fromSnapshot(savedSnapshot)
                ?: return@withLock emitFailure(GameError.DataCorrupted())
            state = restoredState
            lastCompletedSignature = if (restoredState.isMatchCompleted) restoredState.sessionId else null
            val result = GameResult.success(savedSnapshot)
            stateFlow.tryEmit(result)
            startTimerLockedIfNeeded()
            result
        }
    }

    override suspend fun submitGesture(gesture: String): GameResult<GameSnapshot> = withContext(ioDispatcher) {
        mutex.withLock {
            val current = state ?: return@withLock emitFailure(GameError.SessionExpired())
            if (current.isMatchCompleted) {
                return@withLock emitFailure(GameError.SessionExpired(current.sessionId))
            }
            if (current.isRoundCompleted) {
                return@withLock emitFailure(GameError.InvalidMove("Раунд уже завершён"))
            }
            val normalized = gesture.lowercase()
            var updated = current.withPlayerGesture(normalized)
            if (updated.opponentGesture == null) {
                updated = updated.withOpponentGesture(randomGesture())
            }
            updated = concludeRound(updated)
            state = updated
            cancelTimerLocked()
            emitStateLocked(updated)
        }
    }

    override suspend fun restartRound(): GameResult<GameSnapshot> = withContext(ioDispatcher) {
        mutex.withLock {
            val current = state ?: return@withLock emitFailure(GameError.SessionExpired())
            if (current.isMatchCompleted) {
                return@withLock emitFailure(GameError.InvalidMove("Матч уже завершён"))
            }
            val restarted = current.restartRound()
            state = restarted
            cancelTimerLocked()
            val result = emitStateLocked(restarted)
            startTimerLockedIfNeeded()
            result
        }
    }

    override suspend fun confirmResult(): GameResult<GameSnapshot> = withContext(ioDispatcher) {
        mutex.withLock {
            val current = state ?: return@withLock emitFailure(GameError.SessionExpired())
            if (!current.isRoundCompleted) {
                return@withLock emitFailure(GameError.InvalidMove("Раунд ещё продолжается"))
            }
            if (current.isMatchCompleted) {
                val snapshot = current.toSnapshot()
                persistCompletion(snapshot)
                stateFlow.tryEmit(GameResult.success(snapshot))
                repository.clearActive()
                cancelTimerLocked()
                return@withLock GameResult.success(snapshot)
            }
            val next = current.startNextRound()
            state = next
            cancelTimerLocked()
            val result = emitStateLocked(next)
            startTimerLockedIfNeeded()
            result
        }
    }

    override suspend fun abandonGame() {
        withContext(ioDispatcher) {
            mutex.withLock {
                cancelTimerLocked()
                state = null
                repository.clearActive()
                stateFlow.tryEmit(GameResult.failure(GameError.SessionExpired()))
            }
        }
    }

    private suspend fun emitStateLocked(engineState: EngineState): GameResult<GameSnapshot> {
        val snapshot = engineState.toSnapshot()
        repository.saveActive(snapshot)
        if (snapshot.isMatchCompleted) {
            persistCompletion(snapshot)
        }
        val result = GameResult.success(snapshot)
        stateFlow.tryEmit(result)
        return result
    }

    private fun emitFailure(error: GameError): GameResult<GameSnapshot> {
        val failure = GameResult.failure(error)
        stateFlow.tryEmit(failure)
        return failure
    }

    private fun randomGesture(): String = GESTURES[random.nextInt(GESTURES.size)]

    private fun concludeRound(state: EngineState): EngineState {
        val opponent = state.opponentGesture ?: randomGesture()
        val normalizedOpponent = opponent.lowercase()
        val normalizedPlayer = state.playerGesture?.lowercase()
        val outcome = determineOutcome(normalizedPlayer, normalizedOpponent)
        val playerScore = state.playerScore + outcome.playerDelta
        val opponentScore = state.opponentScore + outcome.opponentDelta
        val matchCompleted = playerScore >= ROUNDS_TO_WIN || opponentScore >= ROUNDS_TO_WIN
        val matchResult = if (matchCompleted) {
            when {
                playerScore > opponentScore -> Outcome.PLAYER_WIN.code
                opponentScore > playerScore -> Outcome.PLAYER_LOSS.code
                else -> Outcome.DRAW.code
            }
        } else {
            null
        }
        return state.copy(
            playerGesture = normalizedPlayer,
            opponentGesture = normalizedOpponent,
            playerScore = playerScore,
            opponentScore = opponentScore,
            roundResult = outcome.code,
            matchResult = matchResult,
            isRoundCompleted = true,
            isMatchCompleted = matchCompleted,
            remainingMillis = 0L
        )
    }

    private fun determineOutcome(playerGesture: String?, opponentGesture: String): Outcome {
        val player = playerGesture?.takeIf { it in VALID_GESTURES } ?: return Outcome.DRAW
        val opponent = opponentGesture.takeIf { it in VALID_GESTURES } ?: return Outcome.DRAW
        return if (player == opponent) {
            Outcome.DRAW
        } else if (
            (player == "rock" && opponent == "scissors") ||
                (player == "scissors" && opponent == "paper") ||
                (player == "paper" && opponent == "rock")
        ) {
            Outcome.PLAYER_WIN
        } else {
            Outcome.PLAYER_LOSS
        }
    }

    private suspend fun persistCompletion(snapshot: GameSnapshot) {
        if (lastCompletedSignature == snapshot.sessionId) return
        repository.saveCompleted(snapshot)
        lastCompletedSignature = snapshot.sessionId
    }

    private fun cancelTimerLocked() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun startTimerLockedIfNeeded() {
        val current = state ?: return
        if (current.isRoundCompleted || current.isMatchCompleted) {
            cancelTimerLocked()
            return
        }
        if (timerJob?.isActive == true) return
        timerJob = scope.launch {
            while (true) {
                delay(TIMER_TICK_MILLIS)
                mutex.withLock {
                    val running = state ?: return@withLock
                    if (running.isRoundCompleted || running.isMatchCompleted) {
                        cancelTimerLocked()
                        return@withLock
                    }
                    val ticked = running.tick(TIMER_TICK_MILLIS)
                    var nextState = ticked
                    if (ticked.remainingMillis == 0L) {
                        nextState = concludeRound(
                            if (ticked.opponentGesture == null) ticked.withOpponentGesture(randomGesture()) else ticked
                        )
                    }
                    state = nextState
                    emitStateLocked(nextState)
                    if (nextState.isRoundCompleted || nextState.isMatchCompleted) {
                        cancelTimerLocked()
                    }
                }
            }
        }
    }

    private enum class Outcome(val code: String, val playerDelta: Int, val opponentDelta: Int) {
        PLAYER_WIN("win", 1, 0),
        PLAYER_LOSS("loss", 0, 1),
        DRAW("draw", 0, 0);
    }

    private data class EngineState(
        val sessionId: String,
        val round: Int,
        val playerGesture: String?,
        val opponentGesture: String?,
        val remainingMillis: Long,
        val playerScore: Int,
        val opponentScore: Int,
        val roundResult: String?,
        val matchResult: String?,
        val isRoundCompleted: Boolean,
        val isMatchCompleted: Boolean
    ) {
        fun withPlayerGesture(gesture: String?): EngineState = copy(playerGesture = gesture)

        fun withOpponentGesture(gesture: String?): EngineState = copy(opponentGesture = gesture)

        fun restartRound(): EngineState = copy(
            playerGesture = null,
            opponentGesture = null,
            roundResult = null,
            matchResult = null,
            isRoundCompleted = false,
            remainingMillis = ROUND_DURATION_MILLIS
        )

        fun startNextRound(): EngineState = copy(
            round = round + 1,
            playerGesture = null,
            opponentGesture = null,
            roundResult = null,
            matchResult = null,
            isRoundCompleted = false,
            remainingMillis = ROUND_DURATION_MILLIS
        )

        fun tick(delta: Long): EngineState = copy(
            remainingMillis = (remainingMillis - delta).coerceAtLeast(0L)
        )

        fun toSnapshot(): GameSnapshot = GameSnapshot(
            sessionId = sessionId,
            round = round,
            playerGesture = playerGesture,
            opponentGesture = opponentGesture,
            remainingMillis = remainingMillis,
            playerScore = playerScore,
            opponentScore = opponentScore,
            roundResult = roundResult,
            matchResult = matchResult,
            isRoundCompleted = isRoundCompleted,
            isMatchCompleted = isMatchCompleted
        )

        companion object {
            fun new(): EngineState = EngineState(
                sessionId = UUID.randomUUID().toString(),
                round = 1,
                playerGesture = null,
                opponentGesture = null,
                remainingMillis = ROUND_DURATION_MILLIS,
                playerScore = 0,
                opponentScore = 0,
                roundResult = null,
                matchResult = null,
                isRoundCompleted = false,
                isMatchCompleted = false
            )

            fun fromSnapshot(snapshot: GameSnapshot): EngineState? {
                return EngineState(
                    sessionId = snapshot.sessionId,
                    round = snapshot.round,
                    playerGesture = snapshot.playerGesture,
                    opponentGesture = snapshot.opponentGesture,
                    remainingMillis = snapshot.remainingMillis,
                    playerScore = snapshot.playerScore,
                    opponentScore = snapshot.opponentScore,
                    roundResult = snapshot.roundResult,
                    matchResult = snapshot.matchResult,
                    isRoundCompleted = snapshot.isRoundCompleted,
                    isMatchCompleted = snapshot.isMatchCompleted
                )
            }
        }
    }

    companion object {
        private const val ROUND_DURATION_MILLIS = 10_000L
        private const val TIMER_TICK_MILLIS = 1_000L
        private const val ROUNDS_TO_WIN = 3
        private val GESTURES = listOf("rock", "paper", "scissors")
        private val VALID_GESTURES = GESTURES.toSet()
    }
}
