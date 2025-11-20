package com.example.minitasker.ui.screen.taskdetail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.minitasker.data.model.AttachmentDto
import com.example.minitasker.data.model.CommentDto
import com.example.minitasker.data.model.TaskPriority
import com.example.minitasker.data.model.TaskRequest
import com.example.minitasker.data.model.TaskResponseDto
import com.example.minitasker.data.model.TaskStatus
import com.example.minitasker.ui.theme.MiniTaskerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(taskId: Long, viewModel: TaskDetailViewModel) {
    val state by viewModel.state.collectAsState()
    var commentText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.uploadAttachment(taskId, uri)
        }
    }

    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    TaskDetailContent(
        state = state,
        commentText = commentText,
        onCommentTextChange = { commentText = it },
        onStatusSelected = { status ->
            state.task?.let { task ->
                viewModel.updateTask(taskId, task.toRequest(status = status))
            }
        },
        onPrioritySelected = { priority ->
            state.task?.let { task ->
                viewModel.updateTask(taskId, task.toRequest(priority = priority))
            }
        },
        onAddComment = {
            if (commentText.isNotBlank()) {
                viewModel.addComment(taskId, commentText)
                commentText = ""
            }
        },
        onAttachClick = { filePicker.launch("*/*") },
        onAttachmentClick = { attachment ->
            val url = viewModel.resolveAttachmentUrl(attachment)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "Не удалось открыть файл", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDetailContent(
    state: TaskDetailUiState,
    commentText: String,
    onCommentTextChange: (String) -> Unit,
    onStatusSelected: (TaskStatus) -> Unit,
    onPrioritySelected: (TaskPriority) -> Unit,
    onAddComment: () -> Unit,
    onAttachClick: () -> Unit,
    onAttachmentClick: (AttachmentDto) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(title = { Text(state.task?.title ?: "Задача") })
        }
    ) { paddingValues ->
        state.task?.let { task ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    TaskInfoCard(task = task, onStatusSelected = onStatusSelected, onPrioritySelected = onPrioritySelected)
                }
                item {
                    CommentsCard(comments = task.comments)
                }
                item {
                    AttachmentsCard(
                        attachments = task.attachments,
                        onAttachClick = onAttachClick,
                        onAttachmentClick = onAttachmentClick
                    )
                }
                item {
                    CommentInputCard(commentText = commentText, onCommentTextChange = onCommentTextChange, onAddComment = onAddComment)
                }
                state.error?.let { message ->
                    item {
                        Text(text = message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.loading -> CircularProgressIndicator()
                state.error != null -> Text(text = state.error, color = MaterialTheme.colorScheme.error)
                else -> Text(text = "Загрузка...")
            }
        }
    }
}

@Composable
private fun TaskInfoCard(
    task: TaskResponseDto,
    onStatusSelected: (TaskStatus) -> Unit,
    onPrioritySelected: (TaskPriority) -> Unit
) {
    ElevatedCard(shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = task.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = task.description ?: "Нет описания", style = MaterialTheme.typography.bodyMedium)
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
            Text(text = "Статус", style = MaterialTheme.typography.labelLarge)
            StatusSelectionRow(selected = task.status, onStatusSelected = onStatusSelected)
            Text(text = "Приоритет", style = MaterialTheme.typography.labelLarge)
            PrioritySelectionRow(selected = task.priority, onPrioritySelected = onPrioritySelected)
        }
    }
}

@Composable
private fun StatusSelectionRow(selected: TaskStatus, onStatusSelected: (TaskStatus) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        TaskStatus.values().forEach { status ->
            FilterChip(
                selected = status == selected,
                onClick = { onStatusSelected(status) },
                label = { Text(status.toDisplayName()) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = when (status) {
                        TaskStatus.TODO -> MaterialTheme.colorScheme.surfaceVariant
                        TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondaryContainer
                        TaskStatus.DONE -> MaterialTheme.colorScheme.tertiaryContainer
                    },
                    selectedLabelColor = when (status) {
                        TaskStatus.TODO -> MaterialTheme.colorScheme.onSurfaceVariant
                        TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onSecondaryContainer
                        TaskStatus.DONE -> MaterialTheme.colorScheme.onTertiaryContainer
                    }
                )
            )
        }
    }
}

@Composable
private fun PrioritySelectionRow(selected: TaskPriority, onPrioritySelected: (TaskPriority) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        TaskPriority.values().forEach { priority ->
            FilterChip(
                selected = priority == selected,
                onClick = { onPrioritySelected(priority) },
                label = { Text(priority.toDisplayName()) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = when (priority) {
                        TaskPriority.LOW -> MaterialTheme.colorScheme.surfaceVariant
                        TaskPriority.MEDIUM -> MaterialTheme.colorScheme.primaryContainer
                        TaskPriority.HIGH -> MaterialTheme.colorScheme.errorContainer
                    },
                    selectedLabelColor = when (priority) {
                        TaskPriority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                        TaskPriority.MEDIUM -> MaterialTheme.colorScheme.onPrimaryContainer
                        TaskPriority.HIGH -> MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            )
        }
    }
}

@Composable
private fun CommentsCard(comments: List<CommentDto>) {
    ElevatedCard(shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Комментарии", style = MaterialTheme.typography.titleMedium)
            if (comments.isEmpty()) {
                Text(text = "Комментарии отсутствуют", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                comments.forEachIndexed { index, comment ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = comment.authorName, fontWeight = FontWeight.SemiBold)
                        Text(text = comment.text)
                        Text(text = comment.createdAt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (index != comments.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentsCard(
    attachments: List<AttachmentDto>,
    onAttachClick: () -> Unit,
    onAttachmentClick: (AttachmentDto) -> Unit
) {
    ElevatedCard(shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Вложения", style = MaterialTheme.typography.titleMedium)
            if (attachments.isEmpty()) {
                Text(text = "Файлы не прикреплены", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    attachments.forEach { attachment ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAttachmentClick(attachment) }
                        ) {
                            Text(text = attachment.fileName, fontWeight = FontWeight.SemiBold)
                            Text(text = attachment.uploadedAt, style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = attachment.contentType,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            OutlinedButton(onClick = onAttachClick, shape = MaterialTheme.shapes.medium) {
                Icon(imageVector = Icons.Default.AttachFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Прикрепить файл")
            }
        }
    }
}

@Composable
private fun CommentInputCard(commentText: String, onCommentTextChange: (String) -> Unit, onAddComment: () -> Unit) {
    ElevatedCard(shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Добавить комментарий", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = commentText,
                onValueChange = onCommentTextChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Комментарий") },
                shape = MaterialTheme.shapes.medium
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onAddComment, enabled = commentText.isNotBlank()) {
                    Text("Отправить")
                }
            }
        }
    }
}

private fun TaskResponseDto.toRequest(
    status: TaskStatus = this.status,
    priority: TaskPriority = this.priority
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
private fun TaskDetailPreview() {
    MiniTaskerTheme {
        val comments = listOf(
            CommentDto(1, 1, 1, "Анна", "Проверю сегодня", "2024-04-20"),
            CommentDto(2, 1, 2, "Иван", "Жду правки", "2024-04-21")
        )
        val attachments = listOf(
            AttachmentDto(1, 1, "макет.pdf", "application/pdf", "https://example.com", "2024-04-18")
        )
        val task = TaskResponseDto(
            id = 1,
            projectId = 1,
            title = "Собрать релиз",
            description = "Проверить все чек-листы и собрать билд",
            status = TaskStatus.IN_PROGRESS,
            priority = TaskPriority.HIGH,
            assigneeId = 1,
            assigneeName = "Сергей",
            dueDate = "2024-05-01",
            createdAt = "2024-04-10",
            comments = comments,
            attachments = attachments
        )
        TaskDetailContent(
            state = TaskDetailUiState(task = task),
            commentText = "",
            onCommentTextChange = {},
            onStatusSelected = {},
            onPrioritySelected = {},
            onAddComment = {},
            onAttachClick = {},
            onAttachmentClick = {}
        )
    }
}
