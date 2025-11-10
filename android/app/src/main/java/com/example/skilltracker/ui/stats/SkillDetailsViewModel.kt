package com.example.skilltracker.ui.stats

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skilltracker.data.repository.SessionRepository
import com.example.skilltracker.data.repository.StatsRepository
import com.example.skilltracker.domain.model.Session
import com.example.skilltracker.domain.model.SkillDetailsStats
import kotlinx.coroutines.launch

class SkillDetailsViewModel(
    private val statsRepository: StatsRepository = StatsRepository(),
    private val sessionRepository: SessionRepository = SessionRepository()
) : ViewModel() {

    private val _details = MutableLiveData<SkillDetailsStats?>(null)
    val details: LiveData<SkillDetailsStats?> = _details

    private val _sessions = MutableLiveData<List<Session>>(emptyList())
    val sessions: LiveData<List<Session>> = _sessions

    fun loadSkillDetails(skillId: Long) {
        viewModelScope.launch {
            runCatching { statsRepository.getSkillDetailsStats(skillId) }
                .onSuccess { _details.value = it }
                .onFailure { _details.value = null }

            runCatching { sessionRepository.getSessions(skillId = skillId) }
                .onSuccess { _sessions.value = it }
                .onFailure { _sessions.value = emptyList() }
        }
    }
}
