package com.example.minitasker.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.os.Environment
import com.example.minitasker.data.api.ApiService
import com.example.minitasker.data.model.AttachmentDto
import com.example.minitasker.data.model.CommentDto
import com.example.minitasker.data.model.CommentRequest
import com.example.minitasker.data.model.PagedResponse
import com.example.minitasker.data.model.TaskRequest
import com.example.minitasker.data.model.TaskResponseDto
import com.example.minitasker.data.model.TaskSummaryDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

class TaskRepository(
    private val context: Context,
    private val apiService: ApiService,
    private val baseUrl: String
) {

    suspend fun loadTasks(
        projectId: Long,
        status: String?,
        priority: String?,
        assigneeId: Long?,
        page: Int,
        size: Int
    ): PagedResponse<TaskSummaryDto> {
        return apiService.getTasks(projectId, status, priority, assigneeId, page, size)
    }

    suspend fun createTask(projectId: Long, request: TaskRequest): TaskResponseDto {
        return apiService.createTask(projectId, request)
    }

    suspend fun getTask(taskId: Long): TaskResponseDto {
        return apiService.getTask(taskId)
    }

    suspend fun updateTask(taskId: Long, request: TaskRequest): TaskResponseDto {
        return apiService.updateTask(taskId, request)
    }

    suspend fun deleteTask(taskId: Long) {
        apiService.deleteTask(taskId)
    }

    suspend fun takeTask(projectId: Long, taskId: Long): TaskResponseDto {
        return apiService.takeTask(projectId, taskId)
    }

    suspend fun getComments(taskId: Long): List<CommentDto> {
        return apiService.getComments(taskId)
    }

    suspend fun addComment(taskId: Long, text: String): CommentDto {
        return apiService.addComment(taskId, CommentRequest(text))
    }

    suspend fun getAttachments(taskId: Long): List<AttachmentDto> {
        return apiService.getAttachments(taskId)
    }

    suspend fun uploadAttachment(taskId: Long, uri: Uri): AttachmentDto {
        val resolver = context.contentResolver
        val fileName = resolveFileName(resolver, uri) ?: "attachment"
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Не удалось прочитать файл")
        val requestBody: RequestBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", fileName, requestBody)
        return apiService.uploadAttachment(taskId, part)
    }

    fun resolveAttachmentUrl(attachment: AttachmentDto): String {
        val url = attachment.downloadUrl?.takeIf { it.isNotBlank() }
            ?: attachment.url?.takeIf { it.isNotBlank() }
            ?: "/api/tasks/${attachment.taskId ?: ""}/attachments/${attachment.id}"

        return if (url.startsWith("http")) {
            url
        } else {
            baseUrl.trimEnd('/') + url
        }
    }

    suspend fun downloadAttachment(attachment: AttachmentDto): String {
        val fullUrl = resolveAttachmentUrl(attachment)
        val response = apiService.downloadAttachment(fullUrl)
        if (!response.isSuccessful) {
            throw IOException("Не удалось скачать файл: ${response.code()}")
        }
        val body = response.body() ?: throw IOException("Пустой ответ сервера")
        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        val file = File(downloadsDir, attachment.fileName)
        body.byteStream().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }

    private fun resolveFileName(resolver: ContentResolver, uri: Uri): String? {
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                return cursor.getString(nameIndex)
            }
        }
        return null
    }
}
