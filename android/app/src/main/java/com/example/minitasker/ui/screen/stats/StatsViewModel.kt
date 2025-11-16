package com.example.minitasker.ui.screen.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minitasker.data.model.StatsDto
import com.example.minitasker.data.repository.NetworkResult
import com.example.minitasker.data.repository.StatsRepository
import com.example.minitasker.data.repository.safeCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StatsUiState(
    val statusStats: List<StatsDto> = emptyList(),
    val priorityStats: List<StatsDto> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class StatsViewModel(private val repository: StatsRepository) : ViewModel() {

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    fun load(projectId: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            when (val statusResult = safeCall { repository.loadStatusStats(projectId) }) {
                is NetworkResult.Success -> _state.value = _state.value.copy(statusStats = statusResult.data)
                is NetworkResult.Error -> _state.value = _state.value.copy(error = statusResult.message)
                NetworkResult.Loading -> Unit
            }
            when (val priorityResult = safeCall { repository.loadPriorityStats(projectId) }) {
                is NetworkResult.Success -> _state.value = _state.value.copy(priorityStats = priorityResult.data, loading = false)
                is NetworkResult.Error -> _state.value = _state.value.copy(error = priorityResult.message, loading = false)
                NetworkResult.Loading -> Unit
            }
        }
    }
}
