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
        board = boardRows,
        currentPlayer = "",
        turn = turnCount,
        isCompleted = true,
        winner = winner
    )

    private fun GameSnapshot.toViewState(): ResultsViewState = ResultsViewState.Content(
        sessionId = sessionId,
        turnCount = turn,
        winner = winner,
        isDraw = winner == null,
        boardRows = board
    )
}

sealed class ResultsViewState {
    object Empty : ResultsViewState()
    data class Content(
        val sessionId: String,
        val turnCount: Int,
        val winner: String?,
        val isDraw: Boolean,
        val boardRows: List<String>
    ) : ResultsViewState()
}
