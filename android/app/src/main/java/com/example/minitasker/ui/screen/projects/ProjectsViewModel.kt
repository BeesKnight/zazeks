package com.example.minitasker.ui.screen.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minitasker.data.model.ProjectDto
import com.example.minitasker.data.repository.AuthRepository
import com.example.minitasker.data.repository.NetworkResult
import com.example.minitasker.data.repository.ProjectRepository
import com.example.minitasker.data.repository.safeCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProjectsUiState(
    val projects: List<ProjectDto> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false
)

class ProjectsViewModel(
    private val projectRepository: ProjectRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectsUiState())
    val state: StateFlow<ProjectsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            when (val result = safeCall { projectRepository.loadProjects(null, 0, 100) }) {
                is NetworkResult.Success -> _state.value = _state.value.copy(
                    loading = false,
                    projects = result.data.content
                )
                is NetworkResult.Error -> _state.value = _state.value.copy(loading = false, error = result.message)
                NetworkResult.Loading -> _state.value = _state.value.copy(loading = true)
            }
        }
    }

    fun toggleCreateDialog(show: Boolean) {
        _state.value = _state.value.copy(showCreateDialog = show)
    }

    fun createProject(name: String, description: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            when (val result = safeCall { projectRepository.createProject(name, description.ifBlank { null }) }) {
                is NetworkResult.Success -> {
                    val newList = _state.value.projects + result.data
                    _state.value = _state.value.copy(projects = newList, loading = false, showCreateDialog = false)
                }
                is NetworkResult.Error -> _state.value = _state.value.copy(loading = false, error = result.message)
                NetworkResult.Loading -> _state.value = _state.value.copy(loading = true)
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }
}
