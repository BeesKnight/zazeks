package com.example.minitasker.ui.screen.taskdetail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.minitasker.data.model.TaskRequest

@Composable
fun TaskDetailScreen(taskId: Long, viewModel: TaskDetailViewModel) {
    val state by viewModel.state.collectAsState()
    var commentText by remember { mutableStateOf("") }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.uploadAttachment(taskId, uri)
        }
    }

    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        state.task?.let { task ->
            Text(text = task.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = task.description ?: "Нет описания")
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Статус: ${task.status}")
            Text(text = "Приоритет: ${task.priority}")
            task.assigneeName?.let { Text(text = "Исполнитель: $it") }
            task.dueDate?.let { Text(text = "Срок: $it") }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(label = "TODO", selected = task.status == "TODO") {
                    viewModel.updateTask(taskId, task.toRequest(status = "TODO"))
                }
                StatusChip(label = "IN_PROGRESS", selected = task.status == "IN_PROGRESS") {
                    viewModel.updateTask(taskId, task.toRequest(status = "IN_PROGRESS"))
                }
                StatusChip(label = "DONE", selected = task.status == "DONE") {
                    viewModel.updateTask(taskId, task.toRequest(status = "DONE"))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(label = "LOW", selected = task.priority == "LOW") {
                    viewModel.updateTask(taskId, task.toRequest(priority = "LOW"))
                }
                StatusChip(label = "MEDIUM", selected = task.priority == "MEDIUM") {
                    viewModel.updateTask(taskId, task.toRequest(priority = "MEDIUM"))
                }
                StatusChip(label = "HIGH", selected = task.priority == "HIGH") {
                    viewModel.updateTask(taskId, task.toRequest(priority = "HIGH"))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Комментарии", style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false)) {
                items(task.comments) { comment ->
                    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = comment.authorName, fontWeight = FontWeight.SemiBold)
                            Text(text = comment.text)
                            Text(text = comment.createdAt, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Вложения", style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false)) {
                items(task.attachments) { attachment ->
                    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = attachment.fileName, fontWeight = FontWeight.SemiBold)
                            Text(text = attachment.uploadedAt, style = MaterialTheme.typography.labelSmall)
                            Text(text = attachment.url, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Добавить комментарий") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                if (commentText.isNotBlank()) {
                    viewModel.addComment(taskId, commentText)
                    commentText = ""
                }
            }) {
                Text("Отправить")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { filePicker.launch("*") }) {
                Text("Прикрепить файл")
            }
        } ?: Text("Загрузка...")
    }
}

private fun com.example.minitasker.data.model.TaskResponseDto.toRequest(
    status: String = this.status,
    priority: String = this.priority
): TaskRequest {
    return TaskRequest(
        title = this.title,
        description = this.description,
        status = status,
        priority = priority,
        assigneeId = this.assigneeId,
        dueDate = this.dueDate
    )
}

@Composable
private fun StatusChip(label: String, selected: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = if (selected) {
            AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        } else {
            AssistChipDefaults.assistChipColors()
        }
    )
}
