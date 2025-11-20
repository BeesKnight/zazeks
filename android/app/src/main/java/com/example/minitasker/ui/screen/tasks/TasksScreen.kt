package com.example.minitasker.ui.screen.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.minitasker.data.model.TaskPriority
import com.example.minitasker.data.model.TaskStatus
import com.example.minitasker.data.model.TaskSummaryDto
import com.example.minitasker.ui.theme.MiniTaskerTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
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

    TasksScreenContent(
        projectName = projectName,
        state = state,
        newTaskTitle = newTaskTitle,
        onNewTaskTitleChange = { newTaskTitle = it },
        onStatusFilterSelected = { viewModel.updateStatus(it, projectId) },
        onPriorityFilterSelected = { viewModel.updatePriority(it, projectId) },
        onCreateTask = {
            if (newTaskTitle.isNotBlank()) {
                viewModel.createTask(projectId, newTaskTitle.trim())
                newTaskTitle = ""
            }
        },
        onTaskSelected = onTaskSelected,
        onTakeTask = { taskId -> viewModel.takeTask(projectId, taskId) },
        onNewTaskStatusSelected = viewModel::selectNewTaskStatus,
        onNewTaskPrioritySelected = viewModel::selectNewTaskPriority,
        onRefresh = { viewModel.loadTasks(projectId) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Composable
private fun TasksScreenContent(
    projectName: String,
    state: TasksUiState,
    newTaskTitle: String,
    onNewTaskTitleChange: (String) -> Unit,
    onStatusFilterSelected: (TaskStatusFilter) -> Unit,
    onPriorityFilterSelected: (TaskPriorityFilter) -> Unit,
    onCreateTask: () -> Unit,
    onTaskSelected: (Long) -> Unit,
    onTakeTask: (Long) -> Unit,
    onNewTaskStatusSelected: (TaskStatus) -> Unit,
    onNewTaskPrioritySelected: (TaskPriority) -> Unit,
    onRefresh: () -> Unit,
) {
    val listState = rememberLazyListState()
    val refreshState = rememberPullRefreshState(refreshing = state.loading, onRefresh = onRefresh)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Задачи проекта $projectName") })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(refreshState)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
            item {
                FilterSection(
                    title = "Статусы",
                    filters = listOf(
                        TaskStatusFilter.ALL to "Все",
                        TaskStatusFilter.IN_PROGRESS to "В работе",
                        TaskStatusFilter.TODO to "Сделать",
                        TaskStatusFilter.DONE to "Готово"
                    ),
                    selected = state.status,
                    onSelected = onStatusFilterSelected
                )
            }
            item {
                FilterSection(
                    title = "Приоритеты",
                    filters = listOf(
                        TaskPriorityFilter.ALL to "Все приоритеты",
                        TaskPriorityFilter.HIGH to "Высокий",
                        TaskPriorityFilter.MEDIUM to "Средний",
                        TaskPriorityFilter.LOW to "Низкий"
                    ),
                    selected = state.priority,
                    onSelected = onPriorityFilterSelected
                )
            }
            item {
                NewTaskCard(
                    newTaskTitle = newTaskTitle,
                    onTitleChange = onNewTaskTitleChange,
                    selectedStatus = state.newTaskStatus,
                    selectedPriority = state.newTaskPriority,
                    onStatusSelected = onNewTaskStatusSelected,
                    onPrioritySelected = onNewTaskPrioritySelected,
                    onCreateTask = onCreateTask
                )
            }
            if (state.loading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            if (state.items.isEmpty() && !state.loading) {
                item {
                    Text(
                        text = "Задач нет",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
                items(state.items, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        onClick = { onTaskSelected(task.id) },
                        onTakeTask = if (task.status == TaskStatus.TODO) {
                            { onTakeTask(task.id) }
                        } else {
                            null
                        }
                    )
                }
                state.error?.let { message ->
                    item {
                        Text(text = message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            PullRefreshIndicator(
                refreshing = state.loading,
                state = refreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> FilterSection(
    title: String,
    filters: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEach { (value, label) ->
                val isSelected = value == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelected(value) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewTaskCard(
    newTaskTitle: String,
    onTitleChange: (String) -> Unit,
    selectedStatus: TaskStatus,
    selectedPriority: TaskPriority,
    onStatusSelected: (TaskStatus) -> Unit,
    onPrioritySelected: (TaskPriority) -> Unit,
    onCreateTask: () -> Unit,
) {
    ElevatedCard(shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = newTaskTitle,
                onValueChange = onTitleChange,
                label = { Text("Название задачи") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Text(text = "Статус новой задачи", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskStatus.values().forEach { status ->
                    StatusFilterChip(
                        label = status.toDisplayName(),
                        selected = status == selectedStatus,
                        status = status,
                        onClick = { onStatusSelected(status) }
                    )
                }
            }
            Text(text = "Приоритет новой задачи", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskPriority.values().forEach { priority ->
                    PriorityFilterChip(
                        label = priority.toDisplayName(),
                        selected = priority == selectedPriority,
                        priority = priority,
                        onClick = { onPrioritySelected(priority) }
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onCreateTask, enabled = newTaskTitle.isNotBlank()) {
                    Text("Добавить")
                }
            }
        }
    }
}

@Composable
private fun StatusFilterChip(label: String, selected: Boolean, status: TaskStatus, onClick: () -> Unit) {
    val (container, content) = statusChipColors(status)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = container,
            selectedLabelColor = content
        )
    )
}

@Composable
private fun PriorityFilterChip(label: String, selected: Boolean, priority: TaskPriority, onClick: () -> Unit) {
    val (container, content) = priorityChipColors(priority)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = container,
            selectedLabelColor = content
        )
    )
}

@Composable
private fun TaskItem(task: TaskSummaryDto, onClick: () -> Unit, onTakeTask: (() -> Unit)?) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(status = task.status)
                PriorityPill(priority = task.priority)
            }
            task.assigneeName?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(text = "Исполнитель: $it", style = MaterialTheme.typography.bodySmall)
                }
            }
            task.dueDate?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text(text = "Срок: $it", style = MaterialTheme.typography.bodySmall)
                }
            }
            onTakeTask?.let { take ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    FilledTonalButton(onClick = take) {
                        Text("Взять задачу")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: TaskStatus) {
    val (container, content) = statusChipColors(status)
    SuggestionChip(
        onClick = {},
        enabled = false,
        label = { Text(status.toDisplayName()) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            disabledContainerColor = container,
            disabledLabelColor = content
        )
    )
}

@Composable
private fun PriorityPill(priority: TaskPriority) {
    val (container, content) = priorityChipColors(priority)
    SuggestionChip(
        onClick = {},
        enabled = false,
        label = { Text(priority.toDisplayName()) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            disabledContainerColor = container,
            disabledLabelColor = content
        )
    )
}

@Composable
private fun statusChipColors(status: TaskStatus): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (status) {
        TaskStatus.TODO -> scheme.surfaceVariant to scheme.onSurfaceVariant
        TaskStatus.IN_PROGRESS -> scheme.secondaryContainer to scheme.onSecondaryContainer
        TaskStatus.DONE -> scheme.tertiaryContainer to scheme.onTertiaryContainer
    }
}

@Composable
private fun priorityChipColors(priority: TaskPriority): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (priority) {
        TaskPriority.LOW -> scheme.surfaceVariant to scheme.onSurfaceVariant
        TaskPriority.MEDIUM -> scheme.primaryContainer to scheme.onPrimaryContainer
        TaskPriority.HIGH -> scheme.errorContainer to scheme.onErrorContainer
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

@Preview(showBackground = true)
@Composable
private fun TasksScreenPreview() {
    MiniTaskerTheme {
        val tasks = listOf(
            TaskSummaryDto(1, "Сверстать экран", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, "2024-05-10", "Мария"),
            TaskSummaryDto(2, "Подготовить отчёт", TaskStatus.TODO, TaskPriority.MEDIUM, null, null),
            TaskSummaryDto(3, "Протестировать релиз", TaskStatus.DONE, TaskPriority.LOW, "2024-04-28", "Иван")
        )
        TasksScreenContent(
            projectName = "Проект X",
            state = TasksUiState(items = tasks),
            newTaskTitle = "",
            onNewTaskTitleChange = {},
            onStatusFilterSelected = {},
            onPriorityFilterSelected = {},
            onCreateTask = {},
            onTaskSelected = {},
            onTakeTask = {},
            onNewTaskStatusSelected = {},
            onNewTaskPrioritySelected = {}
        )
    }
}
