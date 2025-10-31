package com.example.zazeks.ui.menu

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zazeks.R
import com.example.zazeks.domain.game.GameSnapshot
import com.example.zazeks.domain.game.GetActiveGameSnapshotUseCase
import com.example.zazeks.domain.game.GetLastCompletedGameUseCase
import com.example.zazeks.domain.results.GetUserGameRoundsUseCase
import com.example.zazeks.domain.results.ResultMode
import com.example.zazeks.domain.results.RoundResult
import com.example.zazeks.infra.auth.AuthTokenStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MainMenuViewModel @Inject constructor(
    private val getActiveGameSnapshot: GetActiveGameSnapshotUseCase,
    private val getLastCompletedGame: GetLastCompletedGameUseCase,
    private val getUserGameRounds: GetUserGameRoundsUseCase,
    private val authTokenStorage: AuthTokenStorage,
) : ViewModel() {

    private val mutableState = MutableLiveData(MainMenuViewState())
    val state: LiveData<MainMenuViewState> = mutableState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val current = mutableState.value ?: MainMenuViewState()
            mutableState.value = current.copy(isLoading = true, errorMessageRes = null)

            val active = getActiveGameSnapshot()
            val lastCompleted = getLastCompletedGame()
            val session = authTokenStorage.currentSession()

            val (topResults, errorRes) = session?.userId?.let { userId ->
                runCatching { getUserGameRounds(userId) }
                    .map { results -> results.take(MAX_TOP_RESULTS) }
                    .fold(
                        onSuccess = { it to null },
                        onFailure = { emptyList<RoundResult>() to R.string.menu_top_results_error }
                    )
            } ?: (emptyList<RoundResult>() to null)

            val stateValue = MainMenuViewState(
                profileUserId = session?.userId,
                canResume = active?.isMatchCompleted?.not() == true,
                lastOffline = lastCompleted?.takeIf { it.isMatchCompleted }?.toRoundResult(),
                topResults = topResults,
                isLoading = false,
                errorMessageRes = errorRes,
            )
            mutableState.value = stateValue
        }
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

    companion object {
        private const val MAX_TOP_RESULTS = 3
    }
}

data class MainMenuViewState(
    val profileUserId: Int? = null,
    val canResume: Boolean = false,
    val lastOffline: RoundResult? = null,
    val topResults: List<RoundResult> = emptyList(),
    val isLoading: Boolean = false,
    @StringRes val errorMessageRes: Int? = null,
)
