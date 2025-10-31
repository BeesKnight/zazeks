package com.example.zazeks.data.game

import com.example.zazeks.domain.game.GameEngine
import com.example.zazeks.domain.game.GameError
import com.example.zazeks.domain.game.GameResult
import com.example.zazeks.domain.game.GameSnapshot
import com.example.zazeks.domain.game.GameStateRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private var state: EngineState? = null

    override fun observeGame(): Flow<GameResult<GameSnapshot>> = stateFlow.asSharedFlow()

    override suspend fun startNewGame(): GameResult<GameSnapshot> = withContext(ioDispatcher) {
        mutex.withLock {
            repository.clearActive()
            val freshState = EngineState.new()
            state = freshState
            val snapshot = freshState.toSnapshot()
            repository.saveActive(snapshot)
            val result = GameResult.success(snapshot)
            stateFlow.tryEmit(result)
            result
        }
    }

    override suspend fun resumeLastGame(): GameResult<GameSnapshot> = withContext(ioDispatcher) {
        mutex.withLock {
            val saved = repository.loadActive()
                ?: run {
                    val failure = GameResult.failure(GameError.SessionExpired())
                    stateFlow.tryEmit(failure)
                    return@withLock failure
                }
            val restoredState = EngineState.fromSnapshot(saved)
                ?: run {
                    val failure = GameResult.failure(GameError.DataCorrupted())
                    stateFlow.tryEmit(failure)
                    return@withLock failure
                }
            state = restoredState
            val result = GameResult.success(saved)
            stateFlow.tryEmit(result)
            result
        }
    }

    override suspend fun playMove(row: Int, column: Int): GameResult<GameSnapshot> = withContext(ioDispatcher) {
        mutex.withLock {
            val current = state ?: return@withLock GameResult.failure(GameError.SessionExpired())
            if (current.isCompleted) {
                return@withLock GameResult.failure(GameError.SessionExpired(current.sessionId))
            }
            if (!current.isWithinBoard(row, column)) {
                return@withLock GameResult.failure(GameError.InvalidMove("Координаты вне доски"))
            }
            if (!current.isCellFree(row, column)) {
                return@withLock GameResult.failure(GameError.InvalidMove("Клетка уже занята"))
            }
            val updated = current.applyMove(row, column)
            state = updated
            val snapshot = updated.toSnapshot()
            repository.saveActive(snapshot)
            if (snapshot.isCompleted) {
                repository.saveCompleted(snapshot)
                repository.clearActive()
            }
            val result = GameResult.success(snapshot)
            stateFlow.tryEmit(result)
            result
        }
    }

    override suspend fun abandonGame() {
        withContext(ioDispatcher) {
            mutex.withLock {
                state = null
                repository.clearActive()
                stateFlow.tryEmit(GameResult.failure(GameError.SessionExpired()))
            }
        }
    }

    private data class EngineState(
        val sessionId: String,
        val board: Array<Array<Player?>>, // row x column
        val currentPlayer: Player,
        val turn: Int,
        val isCompleted: Boolean,
        val winner: Player?
    ) {
        fun isWithinBoard(row: Int, column: Int): Boolean =
            row in board.indices && column in board[row].indices

        fun isCellFree(row: Int, column: Int): Boolean = board[row][column] == null

        fun applyMove(row: Int, column: Int): EngineState {
            val nextBoard = board.map { it.copyOf() }.toTypedArray()
            nextBoard[row][column] = currentPlayer
            val nextTurn = turn + 1
            val nextWinner = determineWinner(nextBoard)
            val completed = nextWinner != null || nextBoard.all { rowCells -> rowCells.all { it != null } }
            val nextPlayer = if (completed) currentPlayer else currentPlayer.other()
            return copy(
                board = nextBoard,
                currentPlayer = nextPlayer,
                turn = nextTurn,
                isCompleted = completed,
                winner = nextWinner
            )
        }

        fun toSnapshot(): GameSnapshot = GameSnapshot(
            sessionId = sessionId,
            board = board.map { row -> row.joinToString("") { it?.symbol?.toString() ?: " " } },
            currentPlayer = currentPlayer.symbol.toString(),
            turn = turn,
            isCompleted = isCompleted,
            winner = winner?.symbol?.toString()
        )

        companion object {
            private const val BOARD_SIZE = 3

            fun new(): EngineState = EngineState(
                sessionId = UUID.randomUUID().toString(),
                board = Array(BOARD_SIZE) { arrayOfNulls<Player?>(BOARD_SIZE) },
                currentPlayer = Player.X,
                turn = 1,
                isCompleted = false,
                winner = null
            )

            fun fromSnapshot(snapshot: GameSnapshot): EngineState? {
                if (snapshot.board.isEmpty()) return null
                val boardSize = snapshot.board.size
                val boardArrays = Array(boardSize) { rowIndex ->
                    val rowString = snapshot.board[rowIndex]
                    Array(boardSize) { columnIndex ->
                        rowString.getOrNull(columnIndex)?.toPlayer()
                    }
                }
                val currentPlayer = snapshot.currentPlayer.singleOrNull()?.toPlayer() ?: return null
                val winner = snapshot.winner?.singleOrNull()?.toPlayer()
                return EngineState(
                    sessionId = snapshot.sessionId,
                    board = boardArrays,
                    currentPlayer = currentPlayer,
                    turn = snapshot.turn,
                    isCompleted = snapshot.isCompleted,
                    winner = winner
                )
            }

            private fun Char.toPlayer(): Player? = when (this) {
                Player.X.symbol -> Player.X
                Player.O.symbol -> Player.O
                else -> null
            }
        }

        private fun determineWinner(board: Array<Array<Player?>>): Player? {
            val size = board.size
            val lines = mutableListOf<List<Player?>>()
            lines.addAll(board.map { it.toList() })
            lines.addAll((0 until size).map { column -> board.map { row -> row[column] } })
            lines.add((0 until size).map { index -> board[index][index] })
            lines.add((0 until size).map { index -> board[index][size - index - 1] })
            return lines.firstOrNull { line -> line.all { it == Player.X } }?.first()
                ?: lines.firstOrNull { line -> line.all { it == Player.O } }?.first()
        }
    }

    private enum class Player(val symbol: Char) {
        X('X'),
        O('O');

        fun other(): Player = if (this == X) O else X
    }
}
