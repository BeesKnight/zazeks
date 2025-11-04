package com.example.zazeks.ui.online

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zazeks.core.gestures.normalizeGesture
import com.example.zazeks.data.multiplayer.BattleResultRequest
import com.example.zazeks.data.multiplayer.MultiplayerStatsRepository
import com.example.zazeks.data.multiplayer.OnlineMatchEvent
import com.example.zazeks.data.multiplayer.OnlineMatchRepository
import com.example.zazeks.di.IoDispatcher
import com.example.zazeks.domain.results.ResultMode
import com.example.zazeks.infra.ml.GameFrame
import com.example.zazeks.infra.ml.NeuralModelBridge
import com.example.zazeks.ui.common.Event
import com.example.zazeks.ui.offline.BoundingBoxUiModel
import com.example.zazeks.ui.offline.DetectionUiModel
import com.example.zazeks.ui.offline.GameResultArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class OnlineMatchViewModel @Inject constructor(
    private val repository: OnlineMatchRepository,
    private val neuralModelBridge: NeuralModelBridge,
    private val statsRepository: MultiplayerStatsRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val mutableState = MutableLiveData<OnlineMatchViewState>(OnlineMatchViewState.Loading)
    val state: LiveData<OnlineMatchViewState> = mutableState

    private val mutableEffects = MutableLiveData<Event<OnlineMatchEffect>>()
    val effects: LiveData<Event<OnlineMatchEffect>> = mutableEffects

    private var currentSession = OnlineMatchUiModel(statusMessage = null)
    private var detectionState = DetectionUiModel()
    private var detectionJob: Job? = null
    private var timerJob: Job? = null
    private var lastValidGesture: String? = null
    private var lastResult: GameResultArgs? = null

    init {
        repository.events
            .onEach { event -> handleEvent(event) }
            .launchIn(viewModelScope)
        repository.connect()
        loadStats()
    }

    fun onReadyClicked() {
        val newReady = !currentSession.playerReady
        updateSession {
            it.copy(
                playerReady = newReady,
                statusMessage = if (newReady) {
                    "Ожидание соперника..."
                } else {
                    "Вы не готовы"
                }
            )
        }
        repository.setReady(newReady)
    }

    fun onPlayAgain() {
        lastResult = null
        lastValidGesture = null
        detectionJob?.cancel()
        updateDetection(DetectionUiModel())
        updateSession {
            it.copy(
                matchResult = null,
                showPlayAgain = false,
                playerGesture = null,
                opponentGesture = null,
                statusMessage = "Поиск соперника",
                playerReady = false,
                opponentReady = false,
            )
        }
        repository.requestPlayAgain()
    }

    fun onShowResults() {
        lastResult?.let { result ->
            mutableEffects.postValue(Event(OnlineMatchEffect.NavigateToResults(result)))
        }
    }

    fun onFrameCaptured(frame: GameFrame) {
        val currentState = mutableState.value
        if (currentState !is OnlineMatchViewState.Content) return
        if (currentSession.matchResult != null || currentSession.blackout) return
        if (detectionJob?.isActive == true) return

        detectionJob = viewModelScope.launch {
            updateDetection(detectionState.copy(isProcessing = true, errorMessage = null, errorMessageRes = null, boundingBox = null, confidence = null))
            try {
                val result = withContext(ioDispatcher) { neuralModelBridge.detect(frame) }
                val gesture = normalizeGesture(result.getGesture())
                if (gesture != null) {
                    lastValidGesture = gesture
                }
                val boundingBox = result.hasDetection()
                    .takeIf { it }
                    ?.let { mapBoundingBox(result) }
                val detection = DetectionUiModel(
                    gesture = gesture ?: detectionState.gesture,
                    isProcessing = false,
                    errorMessage = null,
                    errorMessageRes = null,
                    boundingBox = boundingBox,
                    confidence = result.getConfidence().takeIf { it > 0.0 },
                )
                updateDetection(detection)
                repository.sendGesture(gesture, lastValidGesture)
                if (gesture != null) {
                    updateSession { it.copy(playerGesture = gesture) }
                }
            } catch (throwable: Throwable) {
                updateDetection(
                    detectionState.copy(
                        isProcessing = false,
                        errorMessage = throwable.localizedMessage ?: "Не удалось распознать жест",
                        errorMessageRes = null,
                        boundingBox = null,
                        confidence = null,
                    )
                )
            }
        }
    }

    fun onNavigateBack() {
        repository.disconnect()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val stats = statsRepository.load()
            updateSession { it.copy(stats = stats) }
        }
    }

    private fun handleEvent(event: OnlineMatchEvent) {
        when (event) {
            is OnlineMatchEvent.Connected -> handleConnected()
            is OnlineMatchEvent.MatchFound -> handleMatchFound(event)
            is OnlineMatchEvent.BattleStart -> handleBattleStart(event)
            is OnlineMatchEvent.Blackout -> handleBlackout(event)
            is OnlineMatchEvent.BattleEnd -> handleBattleEnd(event)
            is OnlineMatchEvent.Replay -> handleReplay(event)
            is OnlineMatchEvent.ServerMessage -> handleServerMessage(event)
            is OnlineMatchEvent.Error -> handleError(event.message)
            is OnlineMatchEvent.Closed -> handleClosed(event)
        }
    }

    private fun handleConnected() {
        updateSession { it.copy(statusMessage = "Подключение установлено") }
    }

    private fun handleMatchFound(event: OnlineMatchEvent.MatchFound) {
        updateSession {
            it.copy(
                sessionId = event.sessionId ?: it.sessionId,
                opponentName = event.opponentName ?: it.opponentName,
                playerReady = event.playerReady,
                opponentReady = event.opponentReady,
                round = max(1, event.round),
                playerScore = max(0, event.playerScore),
                opponentScore = max(0, event.opponentScore),
                matchResult = null,
                showPlayAgain = false,
                statusMessage = event.opponentName?.let { name -> "Соперник: ${'$'}name" } ?: "Соперник найден",
            )
        }
    }

    private fun handleBattleStart(event: OnlineMatchEvent.BattleStart) {
        lastValidGesture = null
        detectionJob?.cancel()
        updateDetection(DetectionUiModel())
        startTimer(event.remainingMillis)
        updateSession {
            it.copy(
                round = max(1, event.round),
                remainingMillis = event.remainingMillis,
                statusMessage = "Раунд ${'$'}{event.round}",
                matchResult = null,
                showPlayAgain = false,
                playerGesture = null,
                opponentGesture = null,
                playerReady = false,
                opponentReady = false,
                blackout = event.blackout,
            )
        }
    }

    private fun handleBlackout(event: OnlineMatchEvent.Blackout) {
        updateSession {
            it.copy(
                blackout = event.active,
                statusMessage = event.reason ?: it.statusMessage,
            )
        }
    }

    private fun handleBattleEnd(event: OnlineMatchEvent.BattleEnd) {
        timerJob?.cancel()
        val sessionId = currentSession.sessionId
        val playerGesture = normalizeGesture(event.playerGesture)
        val opponentGesture = normalizeGesture(event.opponentGesture)
        updateSession {
            it.copy(
                matchResult = event.result,
                playerGesture = playerGesture ?: it.playerGesture,
                opponentGesture = opponentGesture ?: it.opponentGesture,
                playerScore = event.playerScore,
                opponentScore = event.opponentScore,
                showPlayAgain = true,
                statusMessage = event.result?.let { result -> mapResultMessage(result) } ?: "Раунд завершён",
                remainingMillis = 0L,
                blackout = false,
            )
        }
        if (sessionId != null) {
            val args = GameResultArgs(
                sessionId = sessionId,
                playerGesture = playerGesture,
                opponentGesture = opponentGesture,
                playerScore = event.playerScore,
                opponentScore = event.opponentScore,
                roundCount = event.round,
                matchResult = event.result,
                mode = ResultMode.ONLINE.name,
            )
            lastResult = args
            viewModelScope.launch {
                val result = repository.submitResult(
                    BattleResultRequest(
                        sessionId = sessionId,
                        result = event.result,
                        playerGesture = playerGesture,
                        opponentGesture = opponentGesture,
                        playerScore = event.playerScore,
                        opponentScore = event.opponentScore,
                        round = event.round,
                    )
                )
                if (result.isFailure) {
                    handleError("Не удалось отправить результат на сервер")
                }
                val stats = statsRepository.recordResult(event.result)
                updateSession { it.copy(stats = stats) }
            }
        }
    }

    private fun handleReplay(event: OnlineMatchEvent.Replay) {
        lastResult = null
        lastValidGesture = null
        detectionJob?.cancel()
        timerJob?.cancel()
        updateDetection(DetectionUiModel())
        updateSession {
            it.copy(
                round = max(1, event.round),
                playerScore = event.playerScore,
                opponentScore = event.opponentScore,
                statusMessage = "Новый раунд готов",
                matchResult = null,
                showPlayAgain = false,
                playerGesture = null,
                opponentGesture = null,
                blackout = false,
            )
        }
    }

    private fun handleServerMessage(event: OnlineMatchEvent.ServerMessage) {
        when (event.type) {
            "ready_state" -> {
                val playerReady = event.payload?.optBoolean("playerReady", currentSession.playerReady)
                    ?: currentSession.playerReady
                val opponentReady = event.payload?.optBoolean("opponentReady", currentSession.opponentReady)
                    ?: currentSession.opponentReady
                updateSession { it.copy(playerReady = playerReady, opponentReady = opponentReady) }
            }
            "opponent_ready" -> {
                val ready = event.payload?.optBoolean("ready", true) ?: true
                updateSession { it.copy(opponentReady = ready) }
            }
            "opponent_gesture" -> {
                val gesture = normalizeGesture(event.payload?.optString("gesture"))
                updateSession { it.copy(opponentGesture = gesture) }
            }
            "status" -> {
                val message = event.payload?.optString("message")
                if (!message.isNullOrBlank()) {
                    updateSession { it.copy(statusMessage = message) }
                }
            }
        }
    }

    private fun handleError(message: String) {
        val current = mutableState.value
        if (current is OnlineMatchViewState.Content) {
            mutableEffects.postValue(Event(OnlineMatchEffect.ShowToast(message)))
        } else {
            mutableState.postValue(OnlineMatchViewState.Error("Ошибка", message))
        }
    }

    private fun handleClosed(event: OnlineMatchEvent.Closed) {
        updateSession {
            it.copy(statusMessage = "Соединение закрыто (${event.code})")
        }
    }

    private fun startTimer(initialMillis: Long) {
        timerJob?.cancel()
        if (initialMillis <= 0L) {
            updateSession { it.copy(remainingMillis = 0L) }
            return
        }
        timerJob = viewModelScope.launch {
            var remaining = initialMillis
            while (remaining >= 0) {
                updateSession { it.copy(remainingMillis = remaining) }
                delay(1000)
                remaining -= 1000
            }
        }
    }

    private fun mapResultMessage(result: String): String = when (result.lowercase()) {
        "win" -> "Вы победили"
        "loss" -> "Вы проиграли"
        "draw" -> "Ничья"
        else -> result
    }

    private fun updateSession(transform: (OnlineMatchUiModel) -> OnlineMatchUiModel) {
        currentSession = transform(currentSession)
        postContent()
    }

    private fun updateDetection(detection: DetectionUiModel) {
        detectionState = detection
        postContent()
    }

    private fun postContent() {
        mutableState.postValue(OnlineMatchViewState.Content(currentSession, detectionState))
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
        val label = normalizeGesture(result.getGesture()) ?: result.getGesture()
        return BoundingBoxUiModel(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            label = label,
            confidence = result.getConfidence().takeIf { it > 0.0 }
        ).clamp()
    }

    override fun onCleared() {
        super.onCleared()
        detectionJob?.cancel()
        timerJob?.cancel()
        repository.disconnect()
    }
}
