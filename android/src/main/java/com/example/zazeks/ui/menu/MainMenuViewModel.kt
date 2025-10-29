package com.example.zazeks.ui.menu

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zazeks.domain.game.GetActiveGameSnapshotUseCase
import com.example.zazeks.domain.game.GetLastCompletedGameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MainMenuViewModel @Inject constructor(
    private val getActiveGameSnapshot: GetActiveGameSnapshotUseCase,
    private val getLastCompletedGame: GetLastCompletedGameUseCase
) : ViewModel() {

    private val mutableState = MutableLiveData(MainMenuViewState())
    val state: LiveData<MainMenuViewState> = mutableState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val active = getActiveGameSnapshot()
            val lastCompleted = getLastCompletedGame()
            val canResume = active != null && !active.isCompleted
            val winner = lastCompleted?.winner
            val hasCompletedGame = lastCompleted?.isCompleted == true
            mutableState.postValue(
                MainMenuViewState(
                    canResume = canResume,
                    lastWinner = winner,
                    hasCompletedGame = hasCompletedGame
                )
            )
        }
    }
}

data class MainMenuViewState(
    val canResume: Boolean = false,
    val lastWinner: String? = null,
    val hasCompletedGame: Boolean = false
)
