package com.example.skilltracker.ui.sessions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skilltracker.data.repository.SessionRepository
import kotlinx.coroutines.launch

class SessionCreateViewModel(
    private val repository: SessionRepository = SessionRepository()
) : ViewModel() {

    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving

    private val _isSuccess = MutableLiveData<Boolean?>(null)
    val isSuccess: LiveData<Boolean?> = _isSuccess

    fun createSession(skillId: Long, durationMinutes: Int, notes: String?) {
        viewModelScope.launch {
            _isSaving.value = true
            runCatching { repository.createSession(skillId, durationMinutes, notes) }
                .onSuccess { _isSuccess.value = true }
                .onFailure { _isSuccess.value = false }
            _isSaving.value = false
        }
    }

    fun resetState() {
        _isSuccess.value = null
    }
}
