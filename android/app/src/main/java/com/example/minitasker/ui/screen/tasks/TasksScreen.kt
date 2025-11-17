package com.example.minitasker.ui.screen.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.minitasker.data.model.TaskPriority
import com.example.minitasker.data.model.TaskStatus
import com.example.minitasker.data.model.TaskSummaryDto

@Composable
fun TasksScreen(
    projectId: Long,
    projectName: String,
    viewModel: TasksViewModel,
    onTaskSelected: (Long) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var newTaskTitle by remember { mutableStateOf("") }

    LaunchedEffect(projectId) {
        viewModel.loadTasks(projectId)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Задачи проекта $projectName", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(label = "Все", selected = state.status == TaskStatusFilter.ALL) {
                viewModel.updateStatus(TaskStatusFilter.ALL, projectId)
            }
            FilterChip(label = "В работе", selected = state.status == TaskStatusFilter.IN_PROGRESS) {
                viewModel.updateStatus(TaskStatusFilter.IN_PROGRESS, projectId)
            }
            FilterChip(label = "Сделать", selected = state.status == TaskStatusFilter.TODO) {
                viewModel.updateStatus(TaskStatusFilter.TODO, projectId)
            }
            FilterChip(label = "Готово", selected = state.status == TaskStatusFilter.DONE) {
                viewModel.updateStatus(TaskStatusFilter.DONE, projectId)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(label = "Все приоритеты", selected = state.priority == TaskPriorityFilter.ALL) {
                viewModel.updatePriority(TaskPriorityFilter.ALL, projectId)
            }
            FilterChip(label = "Высокий", selected = state.priority == TaskPriorityFilter.HIGH) {
                viewModel.updatePriority(TaskPriorityFilter.HIGH, projectId)
            }
            FilterChip(label = "Средний", selected = state.priority == TaskPriorityFilter.MEDIUM) {
                viewModel.updatePriority(TaskPriorityFilter.MEDIUM, projectId)
            }
            FilterChip(label = "Низкий", selected = state.priority == TaskPriorityFilter.LOW) {
                viewModel.updatePriority(TaskPriorityFilter.LOW, projectId)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row { 
            androidx.compose.material3.OutlinedTextField(
                value = newTaskTitle,
                onValueChange = { newTaskTitle = it },
                label = { Text("Новая задача") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (newTaskTitle.isNotBlank()) {
                    viewModel.createTask(projectId, newTaskTitle)
                    newTaskTitle = ""
                }
            }) {
                Text("Добавить")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Статус новой задачи", fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(label = "Сделать", selected = state.newTaskStatus == TaskStatus.TODO) {
                viewModel.selectNewTaskStatus(TaskStatus.TODO)
            }
            FilterChip(label = "Готово", selected = state.newTaskStatus == TaskStatus.DONE) {
                viewModel.selectNewTaskStatus(TaskStatus.DONE)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Приоритет новой задачи", fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(label = "Низкий", selected = state.newTaskPriority == TaskPriority.LOW) {
                viewModel.selectNewTaskPriority(TaskPriority.LOW)
            }
            FilterChip(label = "Средний", selected = state.newTaskPriority == TaskPriority.MEDIUM) {
                viewModel.selectNewTaskPriority(TaskPriority.MEDIUM)
            }
            FilterChip(label = "Высокий", selected = state.newTaskPriority == TaskPriority.HIGH) {
                viewModel.selectNewTaskPriority(TaskPriority.HIGH)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.items) { task ->
                TaskItem(
                    task = task,
                    onClick = { onTaskSelected(task.id) },
                    onTakeTask = if (task.status == TaskStatus.TODO) {
                        { viewModel.takeTask(projectId, task.id) }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.AssistChip(onClick = onClick, label = { Text(label) }, colors = if (selected) {
        androidx.compose.material3.AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
    } else {
        androidx.compose.material3.AssistChipDefaults.assistChipColors()
    })
}

@Composable
private fun TaskItem(task: TaskSummaryDto, onClick: () -> Unit, onTakeTask: (() -> Unit)? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Статус: ${task.status.toDisplayName()} | Приоритет: ${task.priority.toDisplayName()}")
            task.assigneeName?.let { Text(text = "Исполнитель: $it") }
            task.dueDate?.let { Text(text = "Срок: $it") }
            onTakeTask?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = it) {
                    Text("Взять задачу")
                }
            }
        }
    }
}

private fun TaskStatus.toDisplayName(): String = when (this) {
    TaskStatus.TODO -> "Сделать"
    TaskStatus.IN_PROGRESS -> "В работе"
    TaskStatus.DONE -> "Готово"
}

private fun TaskPriority.toDisplayName(): String = when (this) {
    TaskPriority.LOW -> "Низкий"
    TaskPriority.MEDIUM -> "Средний"
    TaskPriority.HIGH -> "Высокий"
}
