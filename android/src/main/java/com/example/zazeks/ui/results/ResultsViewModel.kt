package com.example.zazeks.ui.results

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zazeks.R
import com.example.zazeks.domain.game.GameSnapshot
import com.example.zazeks.domain.game.GetLastCompletedGameUseCase
import com.example.zazeks.domain.results.GetUserGameRoundsUseCase
import com.example.zazeks.domain.results.ResultMode
import com.example.zazeks.domain.results.RoundResult
import com.example.zazeks.infra.auth.AuthTokenStorage
import com.example.zazeks.ui.offline.GameResultArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val getLastCompletedGame: GetLastCompletedGameUseCase,
    private val getUserGameRounds: GetUserGameRoundsUseCase,
    private val authTokenStorage: AuthTokenStorage,
) : ViewModel() {

    private val mutableState = MutableLiveData(ResultsViewState())
    val state: LiveData<ResultsViewState> = mutableState

    private var allItems: List<RoundResult> = emptyList()
    private var hasLoaded = false

    fun load(initialResult: GameResultArgs?) {
        if (hasLoaded && initialResult == null) return

        viewModelScope.launch {
            hasLoaded = true
            val previous = mutableState.value ?: ResultsViewState()
            mutableState.value = previous.copy(isLoading = true, errorMessageRes = null)
            val currentFilter = previous.filter

            val offlineResult = initialResult?.toRoundResult()
                ?: getLastCompletedGame()?.takeIf { it.isMatchCompleted }?.toRoundResult()

            val (onlineResults, errorRes) = loadOnlineResults()

            allItems = buildList {
                offlineResult?.let { add(it) }
                addAll(onlineResults)
            }

            mutableState.value = ResultsViewState(
                isLoading = false,
                filter = currentFilter,
                items = allItems,
                visibleItems = filterItems(currentFilter, allItems),
                errorMessageRes = errorRes,
            )
        }
    }

    fun onFilterSelected(filter: ResultFilter) {
        val visible = filterItems(filter, allItems)
        val current = mutableState.value ?: ResultsViewState()
        mutableState.value = current.copy(filter = filter, visibleItems = visible)
    }

    private suspend fun loadOnlineResults(): Pair<List<RoundResult>, Int?> {
        val session = authTokenStorage.currentSession()
        val userId = session?.userId ?: return emptyList<RoundResult>() to null
        return runCatching { getUserGameRounds(userId) }
            .fold(
                onSuccess = { it to null },
                onFailure = { emptyList<RoundResult>() to R.string.results_error_generic }
            )
    }

    private fun filterItems(filter: ResultFilter, items: List<RoundResult>): List<RoundResult> = when (filter) {
        ResultFilter.ALL -> items
        ResultFilter.OFFLINE -> items.filter { it.mode == ResultMode.OFFLINE }
        ResultFilter.ONLINE -> items.filter { it.mode == ResultMode.ONLINE }
    }

    private fun GameSnapshot.toRoundResult(): RoundResult = RoundResult(
        id = sessionId,
        mode = ResultMode.OFFLINE,
        playerGesture = playerGesture,
        opponentGesture = opponentGesture,
        result = matchResult,
        timestamp = null,
        playerScore = playerScore,
        opponentScore = opponentScore,
    )

    private fun GameResultArgs.toRoundResult(): RoundResult {
        val resolvedMode = mode?.let { value ->
            runCatching { ResultMode.valueOf(value) }.getOrDefault(ResultMode.OFFLINE)
        } ?: ResultMode.OFFLINE
        return RoundResult(
            id = sessionId,
            mode = resolvedMode,
            playerGesture = playerGesture,
            opponentGesture = opponentGesture,
            result = matchResult,
            timestamp = null,
            playerScore = playerScore,
            opponentScore = opponentScore,
        )
    }
}

data class ResultsViewState(
    val isLoading: Boolean = false,
    val filter: ResultFilter = ResultFilter.ALL,
    val items: List<RoundResult> = emptyList(),
    val visibleItems: List<RoundResult> = emptyList(),
    @StringRes val errorMessageRes: Int? = null,
)

enum class ResultFilter {
    ALL,
    OFFLINE,
    ONLINE,
}
