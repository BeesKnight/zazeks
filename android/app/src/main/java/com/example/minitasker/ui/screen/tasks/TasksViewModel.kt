package com.example.minitasker.ui.screen.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minitasker.data.model.TaskRequest
import com.example.minitasker.data.model.TaskSummaryDto
import com.example.minitasker.data.repository.NetworkResult
import com.example.minitasker.data.repository.TaskRepository
import com.example.minitasker.data.repository.safeCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TaskStatusFilter { ALL, TODO, IN_PROGRESS, DONE }

enum class TaskPriorityFilter { ALL, LOW, MEDIUM, HIGH }

data class TasksUiState(
    val items: List<TaskSummaryDto> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val status: TaskStatusFilter = TaskStatusFilter.ALL,
    val priority: TaskPriorityFilter = TaskPriorityFilter.ALL
)

class TasksViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _state = MutableStateFlow(TasksUiState())
    val state: StateFlow<TasksUiState> = _state.asStateFlow()

    fun loadTasks(projectId: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val statusFilter = if (_state.value.status == TaskStatusFilter.ALL) null else _state.value.status.name
            val priorityFilter = if (_state.value.priority == TaskPriorityFilter.ALL) null else _state.value.priority.name
            when (val result = safeCall {
                repository.loadTasks(projectId, statusFilter, priorityFilter, null, 0, 100)
            }) {
                is NetworkResult.Success -> _state.value = _state.value.copy(
                    loading = false,
                    items = result.data.content
                )
                is NetworkResult.Error -> _state.value = _state.value.copy(loading = false, error = result.message)
                NetworkResult.Loading -> _state.value = _state.value.copy(loading = true)
            }
        }
    }

    fun updateStatus(filter: TaskStatusFilter, projectId: Long) {
        _state.value = _state.value.copy(status = filter)
        loadTasks(projectId)
    }

    fun updatePriority(filter: TaskPriorityFilter, projectId: Long) {
        _state.value = _state.value.copy(priority = filter)
        loadTasks(projectId)
    }

    fun createTask(projectId: Long, title: String) {
        viewModelScope.launch {
            val request = TaskRequest(title, description = null, status = "TODO", priority = "MEDIUM", assigneeId = null, dueDate = null)
            when (safeCall { repository.createTask(projectId, request) }) {
                is NetworkResult.Success -> loadTasks(projectId)
                is NetworkResult.Error -> _state.value = _state.value.copy(error = "Не удалось создать задачу")
                NetworkResult.Loading -> Unit
            }
        }
    }
}
