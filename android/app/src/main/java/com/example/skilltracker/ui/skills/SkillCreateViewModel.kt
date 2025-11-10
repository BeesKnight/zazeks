package com.example.skilltracker.ui.skills

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skilltracker.data.repository.SkillRepository
import kotlinx.coroutines.launch

class SkillCreateViewModel(
    private val repository: SkillRepository = SkillRepository()
) : ViewModel() {

    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving

    private val _isSuccess = MutableLiveData<Boolean?>(null)
    val isSuccess: LiveData<Boolean?> = _isSuccess

    fun createSkill(name: String, description: String?, category: String?, color: String?) {
        if (name.isBlank()) {
            _isSuccess.value = false
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val descriptionValue = description?.takeIf { it.isNotBlank() }
            val categoryValue = category?.takeIf { it.isNotBlank() }
            val colorValue = color?.takeIf { it.isNotBlank() }
            runCatching { repository.createSkill(name, descriptionValue, categoryValue, colorValue) }
                .onSuccess { _isSuccess.value = true }
                .onFailure { _isSuccess.value = false }
            _isSaving.value = false
        }
    }

    fun resetState() {
        _isSuccess.value = null
    }
}
