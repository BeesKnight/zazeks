package com.example.zazeks.ui.results

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zazeks.domain.game.GameSnapshot
import com.example.zazeks.domain.game.GetLastCompletedGameUseCase
import com.example.zazeks.ui.game.GameResultArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val getLastCompletedGame: GetLastCompletedGameUseCase
) : ViewModel() {

    private val mutableState = MutableLiveData<ResultsViewState>(ResultsViewState.Empty)
    val state: LiveData<ResultsViewState> = mutableState

    fun load(initialResult: GameResultArgs?) {
        if (mutableState.value !is ResultsViewState.Empty && initialResult == null) return
        viewModelScope.launch {
            val snapshot = initialResult?.toSnapshot() ?: getLastCompletedGame()
            val viewState = snapshot?.toViewState() ?: ResultsViewState.Empty
            mutableState.postValue(viewState)
        }
    }

    private fun GameResultArgs.toSnapshot(): GameSnapshot = GameSnapshot(
        sessionId = sessionId,
        round = roundCount,
        playerGesture = playerGesture,
        opponentGesture = opponentGesture,
        remainingMillis = 0L,
        playerScore = playerScore,
        opponentScore = opponentScore,
        roundResult = null,
        matchResult = matchResult,
        isRoundCompleted = true,
        isMatchCompleted = true
    )

    private fun GameSnapshot.toViewState(): ResultsViewState = ResultsViewState.Content(
        sessionId = sessionId,
        roundsPlayed = round,
        playerGesture = playerGesture,
        opponentGesture = opponentGesture,
        playerScore = playerScore,
        opponentScore = opponentScore,
        matchResult = matchResult
    )
}

sealed class ResultsViewState {
    object Empty : ResultsViewState()
    data class Content(
        val sessionId: String,
        val roundsPlayed: Int,
        val playerGesture: String?,
        val opponentGesture: String?,
        val playerScore: Int,
        val opponentScore: Int,
        val matchResult: String?
    ) : ResultsViewState()
}
