package com.example.skilltracker.ui.stats

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skilltracker.data.repository.StatsRepository
import com.example.skilltracker.domain.model.SkillStats
import kotlinx.coroutines.launch

class StatsViewModel(
    private val repository: StatsRepository = StatsRepository()
) : ViewModel() {

    private val _stats = MutableLiveData<List<SkillStats>>(emptyList())
    val stats: LiveData<List<SkillStats>> = _stats

    fun loadStats() {
        viewModelScope.launch {
            runCatching { repository.getSkillStats() }
                .onSuccess { _stats.value = it }
                .onFailure { _stats.value = emptyList() }
        }
    }
}
