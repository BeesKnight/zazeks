package com.example.minitasker.data.model

import com.squareup.moshi.Json

data class TaskRequest(
    val title: String,
    val description: String?,
    val status: String,
    val priority: String,
    val assigneeId: Long?,
    val dueDate: String?
)

data class TaskSummaryDto(
    val id: Long,
    val title: String,
    val status: String,
    val priority: String,
    val dueDate: String?,
    val assigneeName: String?
)

data class TaskResponseDto(
    val id: Long,
    val projectId: Long,
    val title: String,
    val description: String?,
    val status: String,
    val priority: String,
    val assigneeId: Long?,
    val assigneeName: String?,
    val dueDate: String?,
    val createdAt: String,
    val comments: List<CommentDto>,
    val attachments: List<AttachmentDto>
)

data class CommentDto(
    val id: Long,
    val taskId: Long,
    val authorId: Long,
    val authorName: String,
    val text: String,
    val createdAt: String
)

data class CommentRequest(
    val text: String
)

data class AttachmentDto(
    val id: Long,
    val taskId: Long?,
    val fileName: String,
    val contentType: String,
    val url: String,
    val uploadedAt: String
)

data class StatsDto(
    val key: String,
    val value: Long
)
