package com.example.skilltracker.ui.skills

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skilltracker.data.repository.SkillRepository
import com.example.skilltracker.domain.model.Skill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

class SkillEditViewModel(
    private val repository: SkillRepository = SkillRepository()
) : ViewModel() {

    private val _skill = MutableLiveData<Skill?>(null)
    val skill: LiveData<Skill?> = _skill

    private var currentSkillId: Long? = null

    fun loadSkill(skillId: Long) {
        if (currentSkillId == skillId && _skill.value != null) {
            return
        }
        currentSkillId = skillId
        viewModelScope.launch {
            runCatching { repository.getSkill(skillId) }
                .onSuccess { _skill.value = it }
                .onFailure { _skill.value = null }
        }
    }

    suspend fun updateSkill(name: String, description: String?, category: String?, color: String?): Boolean {
        val id = currentSkillId ?: return false
        return withContext(Dispatchers.IO) {
            runCatching {
                repository.updateSkill(id, name, description, category, color)
            }
                .onSuccess { updated -> _skill.postValue(updated) }
                .isSuccess
        }
    }

    suspend fun archiveSkill(): Boolean {
        val id = currentSkillId ?: return false
        return withContext(Dispatchers.IO) {
            runCatching {
                repository.archiveSkill(id)
            }
                .onSuccess { archived -> _skill.postValue(archived) }
                .isSuccess
        }
    }
}
