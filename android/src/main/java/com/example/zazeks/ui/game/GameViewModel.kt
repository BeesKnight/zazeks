package com.example.zazeks.ui.game

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zazeks.domain.game.GameError
import com.example.zazeks.domain.game.GameResult
import com.example.zazeks.domain.game.GameSnapshot
import com.example.zazeks.domain.game.ObserveGameStateUseCase
import com.example.zazeks.domain.game.PlayMoveUseCase
import com.example.zazeks.domain.game.ResumeGameUseCase
import com.example.zazeks.domain.game.StartNewGameUseCase
import com.example.zazeks.domain.game.AbandonGameUseCase
import com.example.zazeks.ui.common.Event
import com.example.zazeks.ui.game.GameEffect.NavigateToMenu
import com.example.zazeks.ui.game.GameResultArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val observeGameStateUseCase: ObserveGameStateUseCase,
    private val startNewGameUseCase: StartNewGameUseCase,
    private val resumeGameUseCase: ResumeGameUseCase,
    private val playMoveUseCase: PlayMoveUseCase,
    private val abandonGameUseCase: AbandonGameUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val mutableState = MutableLiveData<ViewState>()
    private val mutableEffects = MutableLiveData<Event<GameEffect>>()
    private var observationJob: Job? = null
    private var latestContent: GameUiModel? = savedStateHandle.get(SAVED_STATE_KEY)
    private var lastError: GameError? = null
    private var completionSignature: String? = null

    init {
        latestContent?.let { mutableState.value = ViewState.Content(it) }
    }

    fun observeGameState(): LiveData<ViewState> {
        if (mutableState.value == null) {
            mutableState.value = ViewState.Loading
        }
        if (observationJob == null) {
            observationJob = observeGameStateUseCase()
                .onEach { result -> handleResult(result) }
                .catch { throwable -> mutableState.postValue(mapError(GameError.Unknown(throwable))) }
                .launchIn(viewModelScope)
        }
        return mutableState
    }

    fun effects(): LiveData<Event<GameEffect>> = mutableEffects

    fun onStartNewGame() {
        mutableState.value = ViewState.Loading
        viewModelScope.launch {
            handleResult(startNewGameUseCase())
        }
    }

    fun onResumeGame() {
        viewModelScope.launch {
            mutableState.postValue(ViewState.Loading)
            handleResult(resumeGameUseCase())
        }
    }

    fun onCellSelected(row: Int, column: Int) {
        viewModelScope.launch {
            handleResult(playMoveUseCase(row, column))
        }
    }

    fun onQuitToMenu() {
        viewModelScope.launch {
            abandonGameUseCase()
            mutableEffects.postValue(Event(NavigateToMenu))
        }
    }

    fun onErrorAction() {
        when (val error = lastError) {
            is GameError.SessionExpired -> mutableEffects.postValue(Event(NavigateToMenu))
            else -> {
                val previous = latestContent
                if (previous != null) {
                    mutableState.value = ViewState.Content(previous)
                } else {
                    onResumeGame()
                }
            }
        }
    }

    override fun onCleared() {
        observationJob?.cancel()
        super.onCleared()
    }

    private fun handleResult(result: GameResult<GameSnapshot>) {
        when (result) {
            is GameResult.Success -> handleSnapshot(result.value)
            is GameResult.Failure -> mutableState.postValue(mapError(result.error))
        }
    }

    private fun handleSnapshot(snapshot: GameSnapshot) {
        lastError = null
        val uiModel = snapshot.toUiModel()
        latestContent = uiModel
        savedStateHandle[SAVED_STATE_KEY] = uiModel
        mutableState.postValue(ViewState.Content(uiModel))
        if (snapshot.isCompleted) {
            emitCompletionIfNeeded(snapshot, uiModel)
        } else {
            completionSignature = null
        }
    }

    private fun emitCompletionIfNeeded(snapshot: GameSnapshot, uiModel: GameUiModel) {
        val signature = "${snapshot.sessionId}:${snapshot.turn}"
        if (signature != completionSignature) {
            completionSignature = signature
            mutableEffects.postValue(
                Event(
                    GameEffect.NavigateToResults(
                        GameResultArgs(
                            sessionId = snapshot.sessionId,
                            winner = snapshot.winner,
                            turnCount = snapshot.turn,
                            boardRows = uiModel.boardRows
                        )
                    )
                )
            )
        }
    }

    private fun GameSnapshot.toUiModel(): GameUiModel {
        return GameUiModel(
            sessionId = sessionId,
            boardRows = board,
            currentPlayer = currentPlayer,
            turnCount = turn,
            isCompleted = isCompleted,
            winner = winner
        )
    }

    private fun mapError(error: GameError): ViewState.Error {
        lastError = error
        return when (error) {
            is GameError.InvalidMove -> ViewState.Error(
                title = "Недопустимый ход",
                message = error.reason,
                isRecoverable = true,
                action = ViewState.Error.Action(
                    label = "Изменить ход",
                    analyticsTag = "retry_invalid_move"
                )
            )
            is GameError.SessionExpired -> {
                val hint = error.sessionId?.let { "ID: $it" } ?: "Запустите новую игру."
                ViewState.Error(
                    title = "Сессия завершена",
                    message = "Текущая игровая сессия недоступна. $hint",
                    isRecoverable = false,
                    action = ViewState.Error.Action(
                        label = "В главное меню",
                        analyticsTag = "nav_session_expired"
                    )
                )
            }
            is GameError.Network -> ViewState.Error(
                title = "Проблемы с сетью",
                message = "Не удалось синхронизировать ход. Проверьте подключение и повторите попытку.",
                isRecoverable = true,
                action = ViewState.Error.Action(
                    label = "Повторить",
                    analyticsTag = "retry_network_error"
                )
            )
            is GameError.DataCorrupted -> ViewState.Error(
                title = "Ошибка данных",
                message = "Получено некорректное состояние игры. Требуется перезапуск приложения.",
                isRecoverable = false,
                action = ViewState.Error.Action(
                    label = "Перезапустить",
                    analyticsTag = "restart_corrupted_state"
                )
            )
            is GameError.Unknown -> ViewState.Error(
                title = "Неизвестная ошибка",
                message = "Что-то пошло не так. Попробуйте позже или сообщите в поддержку.",
                isRecoverable = true,
                action = ViewState.Error.Action(
                    label = "Сообщить",
                    analyticsTag = "report_unknown_error"
                )
            )
        }
    }

    companion object {
        private const val SAVED_STATE_KEY = "game_ui_model"
    }
}
