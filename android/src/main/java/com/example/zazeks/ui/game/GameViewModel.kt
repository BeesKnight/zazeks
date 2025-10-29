package com.example.zazeks.ui.game

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zazeks.domain.game.GameError
import com.example.zazeks.domain.game.GameResult
import com.example.zazeks.domain.game.GameSnapshot
import com.example.zazeks.domain.game.ObserveGameStateUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class GameViewModel(
    private val observeGameStateUseCase: ObserveGameStateUseCase
) : ViewModel() {

    private val mutableState = MutableLiveData<ViewState>()
    private var observationJob: Job? = null

    fun observeGameState(): LiveData<ViewState> {
        if (mutableState.value == null) {
            mutableState.value = ViewState.Loading
        }
        if (observationJob == null) {
            observationJob = observeGameStateUseCase()
                .onEach { result -> mutableState.postValue(result.toViewState()) }
                .catch { throwable ->
                    mutableState.postValue(mapError(GameError.Unknown(throwable)))
                }
                .launchIn(viewModelScope)
        }
        return mutableState
    }

    override fun onCleared() {
        observationJob?.cancel()
        super.onCleared()
    }

    private fun GameResult<GameSnapshot>.toViewState(): ViewState = when (this) {
        is GameResult.Success -> ViewState.Content(value.toUiModel())
        is GameResult.Failure -> mapError(error)
    }

    private fun GameSnapshot.toUiModel(): GameUiModel = GameUiModel(
        sessionId = sessionId,
        boardRows = board,
        currentPlayerLabel = "Ход игрока: $currentPlayer",
        turnLabel = "Ход #$turn",
        completionLabel = if (isCompleted) "Партия завершена" else null
    )

    private fun mapError(error: GameError): ViewState.Error = when (error) {
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
