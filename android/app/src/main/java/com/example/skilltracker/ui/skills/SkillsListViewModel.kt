package com.example.skilltracker.ui.skills

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skilltracker.data.repository.SkillRepository
import com.example.skilltracker.domain.model.Skill
import kotlinx.coroutines.launch

class SkillsListViewModel(
    private val repository: SkillRepository = SkillRepository()
) : ViewModel() {

    private val _skills = MutableLiveData<List<Skill>>(emptyList())
    val skills: LiveData<List<Skill>> = _skills

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private var includeArchived: Boolean = false

    init {
        loadSkills()
    }

    fun loadSkills(includeArchived: Boolean = this.includeArchived) {
        this.includeArchived = includeArchived
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { repository.getSkills(includeArchived) }
                .onSuccess { _skills.value = it }
                .onFailure { _skills.value = emptyList() }
            _isLoading.value = false
        }
    }

    fun toggleIncludeArchived(include: Boolean) {
        loadSkills(include)
    }
}
