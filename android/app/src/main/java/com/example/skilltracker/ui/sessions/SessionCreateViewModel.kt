package com.example.skilltracker.ui.sessions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skilltracker.data.repository.SessionRepository
import com.example.skilltracker.data.repository.SkillRepository
import com.example.skilltracker.domain.model.Skill
import java.time.Instant
import kotlinx.coroutines.launch

class SessionCreateViewModel(
    private val sessionRepository: SessionRepository = SessionRepository(),
    private val skillRepository: SkillRepository = SkillRepository()
) : ViewModel() {

    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving

    private val _isSuccess = MutableLiveData<Boolean?>(null)
    val isSuccess: LiveData<Boolean?> = _isSuccess

    private val _skills = MutableLiveData<List<Skill>>(emptyList())
    val skills: LiveData<List<Skill>> = _skills

    init {
        loadSkills()
    }

    fun loadSkills() {
        viewModelScope.launch {
            runCatching { skillRepository.getSkills() }
                .onSuccess { _skills.value = it }
                .onFailure { _skills.value = emptyList() }
        }
    }

    fun createSession(
        skillId: Long,
        durationMinutes: Int,
        notes: String?,
        difficulty: Int?,
        source: String?,
        sessionDate: Instant?
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val notesValue = notes?.takeIf { it.isNotBlank() }
            val sourceValue = source?.takeIf { it.isNotBlank() }
            runCatching {
                sessionRepository.createSession(
                    skillId = skillId,
                    durationMinutes = durationMinutes,
                    notes = notesValue,
                    difficulty = difficulty,
                    source = sourceValue,
                    sessionDate = sessionDate
                )
            }
                .onSuccess { _isSuccess.value = true }
                .onFailure { _isSuccess.value = false }
            _isSaving.value = false
        }
    }

    fun resetState() {
        _isSuccess.value = null
    }
}
