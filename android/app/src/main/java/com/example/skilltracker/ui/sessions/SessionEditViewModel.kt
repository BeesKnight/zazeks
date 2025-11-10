package com.example.skilltracker.ui.sessions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skilltracker.data.repository.SessionRepository
import com.example.skilltracker.data.repository.SkillRepository
import com.example.skilltracker.domain.model.Session
import com.example.skilltracker.domain.model.Skill
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SessionEditViewModel(
    private val sessionRepository: SessionRepository = SessionRepository(),
    private val skillRepository: SkillRepository = SkillRepository()
) : ViewModel() {

    private val _session = MutableLiveData<Session?>(null)
    val session: LiveData<Session?> = _session

    private val _skills = MutableLiveData<List<Skill>>(emptyList())
    val skills: LiveData<List<Skill>> = _skills

    private var currentSessionId: Long? = null

    fun loadData(sessionId: Long) {
        if (currentSessionId == sessionId && _session.value != null && _skills.value?.isNotEmpty() == true) {
            return
        }
        currentSessionId = sessionId
        viewModelScope.launch {
            val skillsResult = runCatching { skillRepository.getSkills(includeArchived = true) }
            if (skillsResult.isSuccess) {
                _skills.value = skillsResult.getOrDefault(emptyList())
            } else {
                _skills.value = emptyList()
            }

            runCatching { sessionRepository.getSession(sessionId) }
                .onSuccess { _session.value = it }
                .onFailure { _session.value = null }
        }
    }

    suspend fun updateSession(
        skillId: Long,
        durationMinutes: Int,
        notes: String?,
        difficulty: Int?,
        source: String?,
        sessionDate: Instant?
    ): Boolean {
        val id = currentSessionId ?: return false
        return withContext(Dispatchers.IO) {
            runCatching {
                sessionRepository.updateSession(id, skillId, durationMinutes, notes, difficulty, source, sessionDate)
            }
                .onSuccess { updated -> _session.postValue(updated) }
                .isSuccess
        }
    }

    suspend fun deleteSession(): Boolean {
        val id = currentSessionId ?: return false
        return withContext(Dispatchers.IO) {
            runCatching {
                sessionRepository.deleteSession(id)
            }.isSuccess
        }
    }
}
