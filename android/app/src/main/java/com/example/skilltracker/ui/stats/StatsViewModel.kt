package com.example.skilltracker.ui.stats

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skilltracker.data.repository.StatsRepository
import com.example.skilltracker.domain.model.OverviewStats
import com.example.skilltracker.domain.model.SkillStats
import kotlinx.coroutines.launch

class StatsViewModel(
    private val repository: StatsRepository = StatsRepository()
) : ViewModel() {

    private val _stats = MutableLiveData<List<SkillStats>>(emptyList())
    val stats: LiveData<List<SkillStats>> = _stats

    private val _overview = MutableLiveData<OverviewStats?>(null)
    val overview: LiveData<OverviewStats?> = _overview

    fun loadStats() {
        viewModelScope.launch {
            runCatching { repository.getOverviewStats() }
                .onSuccess { overview ->
                    _overview.value = overview
                    _stats.value = overview.bySkill.map {
                        SkillStats(it.skillId, it.skillName, it.sessions, it.minutes)
                    }
                }
                .onFailure {
                    _overview.value = null
                    _stats.value = emptyList()
                }
        }
    }
}
