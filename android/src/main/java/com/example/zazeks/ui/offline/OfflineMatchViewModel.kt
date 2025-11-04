package com.example.zazeks.ui.offline

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zazeks.R
import com.example.zazeks.core.gestures.normalizeGesture
import com.example.zazeks.di.IoDispatcher
import com.example.zazeks.domain.game.AbandonGameUseCase
import com.example.zazeks.domain.game.ConfirmRoundResultUseCase
import com.example.zazeks.domain.game.GameError
import com.example.zazeks.domain.game.GameResult
import com.example.zazeks.domain.game.GameSnapshot
import com.example.zazeks.domain.game.ObserveGameStateUseCase
import com.example.zazeks.domain.game.RestartRoundUseCase
import com.example.zazeks.domain.game.ResumeGameUseCase
import com.example.zazeks.domain.game.StartNewGameUseCase
import com.example.zazeks.domain.game.SubmitGestureUseCase
import com.example.zazeks.infra.ml.GameFrame
import com.example.zazeks.domain.results.ResultMode
import com.example.zazeks.infra.ml.NeuralModelBridge
import com.example.zazeks.ui.common.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class OfflineMatchViewModel @Inject constructor(
    private val observeGameStateUseCase: ObserveGameStateUseCase,
    private val startNewGameUseCase: StartNewGameUseCase,
    private val resumeGameUseCase: ResumeGameUseCase,
    private val submitGestureUseCase: SubmitGestureUseCase,
    private val restartRoundUseCase: RestartRoundUseCase,
    private val confirmRoundResultUseCase: ConfirmRoundResultUseCase,
    private val abandonGameUseCase: AbandonGameUseCase,
    private val neuralModelBridge: NeuralModelBridge,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val mutableState = MutableLiveData<ViewState>()
    private val mutableEffects = MutableLiveData<Event<OfflineMatchEffect>>()
    private var observationJob: Job? = null
    private var latestSession: OfflineMatchUiModel? = runCatching {
        savedStateHandle.get<OfflineMatchUiModel>(SAVED_SESSION_KEY)
    }.getOrNull()
    private var latestDetection: DetectionUiModel = runCatching {
        savedStateHandle.get<DetectionUiModel>(SAVED_DETECTION_KEY)
    }.getOrNull() ?: DetectionUiModel()
    private var detectionJob: Job? = null
    private var lastError: GameError? = null
    private var completionSignature: String? = null

    init {
        latestSession?.let { session ->
            mutableState.value = ViewState.Content(session, latestDetection)
        }
    }

    fun observeGameState(): LiveData<ViewState> {
        if (mutableState.value == null) {
            mutableState.value = ViewState.Loading
        }
        if (observationJob == null) {
            observationJob = observeGameStateUseCase()
                .onEach { result -> handleResult(result) }
                .catch { throwable ->
                    mutableState.postValue(mapError(GameError.Unknown(throwable)))
                }
                .launchIn(viewModelScope)
        }
        return mutableState
    }

    fun effects(): LiveData<Event<OfflineMatchEffect>> = mutableEffects

    fun onStartNewMatch() {
        detectionJob?.cancel()
        latestDetection = DetectionUiModel()
        savedStateHandle[SAVED_DETECTION_KEY] = latestDetection
        mutableState.value = ViewState.Loading
        viewModelScope.launch {
            handleResult(startNewGameUseCase())
        }
    }

    fun onResumeMatch() {
        detectionJob?.cancel()
        latestDetection = DetectionUiModel()
        savedStateHandle[SAVED_DETECTION_KEY] = latestDetection
        viewModelScope.launch {
            mutableState.postValue(ViewState.Loading)
            handleResult(resumeGameUseCase())
        }
    }

    fun onReadyAction() {
        val session = latestSession ?: return
        when {
            session.isMatchCompleted || session.isRoundCompleted -> confirmRoundResult()
            else -> submitDetectedGesture()
        }
    }

    fun onRestartRound() {
        detectionJob?.cancel()
        updateDetection(DetectionUiModel())
        viewModelScope.launch {
            handleResult(restartRoundUseCase())
        }
    }

    fun onQuitToMenu() {
        viewModelScope.launch {
            abandonGameUseCase()
            mutableEffects.postValue(Event(OfflineMatchEffect.NavigateToMenu))
        }
    }

    fun onErrorAction() {
        when (val error = lastError) {
            is GameError.SessionExpired -> mutableEffects.postValue(Event(OfflineMatchEffect.NavigateToMenu))
            else -> {
                val session = latestSession
                if (session != null) {
                    mutableState.value = ViewState.Content(session, latestDetection)
                } else {
                    onResumeMatch()
                }
            }
        }
    }

    fun onFrameCaptured(frame: GameFrame) {
        val session = latestSession ?: return
        if (session.isRoundCompleted || session.isMatchCompleted) return
        if (detectionJob?.isActive == true) return
        detectionJob = viewModelScope.launch {
            updateDetection(
                latestDetection.copy(
                    isProcessing = true,
                    errorMessage = null,
                    errorMessageRes = null
                )
            )
            try {
                val result = withContext(ioDispatcher) { neuralModelBridge.detect(frame) }
                val gesture = normalizeGesture(result.getGesture())
                val boundingBox = result.hasDetection()
                    .takeIf { it }
                    ?.let { mapBoundingBox(result) }
                updateDetection(
                    latestDetection.copy(
                        gesture = gesture,
                        isProcessing = false,
                        errorMessage = null,
                        errorMessageRes = null,
                        boundingBox = boundingBox,
                        confidence = result.getConfidence().takeIf { it > 0.0 }
                    )
                )
            } catch (ioException: IOException) {
                updateDetection(
                    latestDetection.copy(
                        isProcessing = false,
                        errorMessage = ioException.localizedMessage,
                        errorMessageRes = R.string.offline_detection_error,
                        boundingBox = null,
                        confidence = null
                    )
                )
            } catch (throwable: Throwable) {
                updateDetection(
                    latestDetection.copy(
                        isProcessing = false,
                        errorMessage = throwable.localizedMessage,
                        errorMessageRes = R.string.offline_detection_error,
                        boundingBox = null,
                        confidence = null
                    )
                )
            }
        }
    }

    fun onCameraPermissionDenied() {
        updateDetection(
            latestDetection.copy(
                isProcessing = false,
                errorMessage = null,
                errorMessageRes = R.string.offline_permission_required
            )
        )
    }

    override fun onCleared() {
        observationJob?.cancel()
        detectionJob?.cancel()
        super.onCleared()
    }

    private fun submitDetectedGesture() {
        val gesture = normalizeGesture(latestDetection.gesture)
        if (gesture == null) {
            updateDetection(
                latestDetection.copy(
                    errorMessage = null,
                    errorMessageRes = R.string.offline_detection_unknown
                )
            )
            return
        }
        detectionJob?.cancel()
        updateDetection(
            latestDetection.copy(
                isProcessing = true,
                errorMessage = null,
                errorMessageRes = null,
                boundingBox = null,
                confidence = null
            )
        )
        viewModelScope.launch {
            handleResult(submitGestureUseCase(gesture))
        }
    }

    private fun confirmRoundResult() {
        viewModelScope.launch {
            handleResult(confirmRoundResultUseCase())
        }
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
        latestSession = uiModel
        savedStateHandle[SAVED_SESSION_KEY] = uiModel

        if (!snapshot.isRoundCompleted && snapshot.playerGesture.isNullOrBlank()) {
            updateDetection(DetectionUiModel())
        } else if (!snapshot.playerGesture.isNullOrBlank()) {
            updateDetection(
                DetectionUiModel(
                    gesture = snapshot.playerGesture,
                    isProcessing = false,
                    errorMessage = null,
                    errorMessageRes = null,
                    boundingBox = null,
                    confidence = null
                )
            )
        } else {
            postContent()
        }

        if (snapshot.isMatchCompleted) {
            emitCompletionIfNeeded(snapshot)
        } else {
            completionSignature = null
        }
    }

    private fun emitCompletionIfNeeded(snapshot: GameSnapshot) {
        val signature = "${snapshot.sessionId}:${snapshot.matchResult}:${snapshot.round}"
        if (signature != completionSignature) {
            completionSignature = signature
            mutableEffects.postValue(
                Event(
                    OfflineMatchEffect.NavigateToResults(
                        GameResultArgs(
                            sessionId = snapshot.sessionId,
                            playerGesture = snapshot.playerGesture,
                            opponentGesture = snapshot.opponentGesture,
                            playerScore = snapshot.playerScore,
                            opponentScore = snapshot.opponentScore,
                            roundCount = snapshot.round,
                            matchResult = snapshot.matchResult,
                            mode = ResultMode.OFFLINE.name
                        )
                    )
                )
            )
        }
    }

    private fun GameSnapshot.toUiModel(): OfflineMatchUiModel {
        return OfflineMatchUiModel(
            sessionId = sessionId,
            round = round,
            playerGesture = playerGesture,
            opponentGesture = opponentGesture,
            remainingSeconds = remainingMillis / 1000.0,
            playerScore = playerScore,
            opponentScore = opponentScore,
            roundResult = roundResult,
            matchResult = matchResult,
            isRoundCompleted = isRoundCompleted,
            isMatchCompleted = isMatchCompleted
        )
    }

    private fun mapError(error: GameError): ViewState.Error {
        lastError = error
        return when (error) {
            is GameError.InvalidMove -> ViewState.Error(
                title = "Неверное действие",
                message = error.reason,
                isRecoverable = true,
                action = ViewState.Error.Action(
                    label = "Попробовать снова",
                    analyticsTag = "retry_invalid_action"
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

    private fun postContent() {
        val session = latestSession ?: return
        mutableState.postValue(ViewState.Content(session, latestDetection))
    }

    private fun updateDetection(detection: DetectionUiModel) {
        latestDetection = detection
        savedStateHandle[SAVED_DETECTION_KEY] = detection
        postContent()
    }

    private fun mapBoundingBox(result: com.example.zazeks.infra.ml.DetectionResult): BoundingBoxUiModel? {
        val raw = result.getBoundingBox()
        if (raw.size != 4) return null
        val width = result.getFrameWidth().takeIf { it > 0 } ?: return null
        val height = result.getFrameHeight().takeIf { it > 0 } ?: return null
        val left = raw[0] / width.toFloat()
        val top = raw[1] / height.toFloat()
        val right = raw[2] / width.toFloat()
        val bottom = raw[3] / height.toFloat()
        val gestureLabel = normalizeGesture(result.getGesture()) ?: result.getGesture()
        return BoundingBoxUiModel(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            label = gestureLabel,
            confidence = result.getConfidence().takeIf { it > 0.0 }
        ).clamp()
    }

    companion object {
        private const val SAVED_SESSION_KEY = "offline_match_session"
        private const val SAVED_DETECTION_KEY = "offline_match_detection"
    }
}
