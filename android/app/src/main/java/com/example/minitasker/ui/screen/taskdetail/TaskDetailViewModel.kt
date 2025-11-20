package com.example.minitasker.ui.screen.taskdetail

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minitasker.data.model.AttachmentDto
import com.example.minitasker.data.model.TaskRequest
import com.example.minitasker.data.model.TaskResponseDto
import com.example.minitasker.data.repository.NetworkResult
import com.example.minitasker.data.repository.TaskRepository
import com.example.minitasker.data.repository.safeCall
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class TaskDetailUiState(
    val task: TaskResponseDto? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val downloadingAttachments: Set<Long> = emptySet()
)

sealed class TaskDetailEvent {
    data class ShowMessage(val message: String) : TaskDetailEvent()
}

class TaskDetailViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _state = MutableStateFlow(TaskDetailUiState())
    val state: StateFlow<TaskDetailUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TaskDetailEvent>()
    val events: SharedFlow<TaskDetailEvent> = _events.asSharedFlow()

    var onTaskChanged: ((Long) -> Unit)? = null

    fun loadTask(taskId: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            when (val result = safeCall { repository.getTask(taskId) }) {
                is NetworkResult.Success -> _state.value = _state.value.copy(task = result.data, loading = false)
                is NetworkResult.Error -> _state.value = _state.value.copy(loading = false, error = result.message)
                NetworkResult.Loading -> _state.value = _state.value.copy(loading = true)
            }
        }
    }

    fun updateTask(taskId: Long, request: TaskRequest) {
        viewModelScope.launch {
            when (val result = safeCall { repository.updateTask(taskId, request) }) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(task = result.data, error = null)
                    onTaskChanged?.invoke(result.data.projectId)
                }
                is NetworkResult.Error -> _state.value = _state.value.copy(error = result.message)
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun addComment(taskId: Long, text: String) {
        viewModelScope.launch {
            when (val result = safeCall { repository.addComment(taskId, text) }) {
                is NetworkResult.Success -> {
                    val task = _state.value.task
                    if (task != null) {
                        val updated = task.copy(comments = task.comments + result.data)
                        _state.value = _state.value.copy(task = updated)
                    } else {
                        loadTask(taskId)
                    }
                }
                is NetworkResult.Error -> _state.value = _state.value.copy(error = result.message)
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun uploadAttachment(taskId: Long, uri: Uri) {
        viewModelScope.launch {
            when (val result = safeCall { repository.uploadAttachment(taskId, uri) }) {
                is NetworkResult.Success -> {
                    val task = _state.value.task
                    if (task != null) {
                        val updated = task.copy(attachments = task.attachments + result.data)
                        _state.value = _state.value.copy(task = updated)
                    } else {
                        loadTask(taskId)
                    }
                }
                is NetworkResult.Error -> _state.value = _state.value.copy(error = result.message)
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun downloadAttachment(attachment: AttachmentDto) {
        viewModelScope.launch {
            _state.update { it.copy(downloadingAttachments = it.downloadingAttachments + attachment.id) }
            when (val result = safeCall { repository.downloadAttachment(attachment) }) {
                is NetworkResult.Success -> {
                    _events.emit(TaskDetailEvent.ShowMessage("Файл ${attachment.fileName} скачан"))
                }
                is NetworkResult.Error -> {
                    _events.emit(TaskDetailEvent.ShowMessage(result.message ?: "Не удалось скачать файл"))
                }
                NetworkResult.Loading -> Unit
            }
            _state.update { it.copy(downloadingAttachments = it.downloadingAttachments - attachment.id) }
        }
    }
}
