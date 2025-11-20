package com.example.minitasker.data.api

import com.example.minitasker.data.model.AttachmentDto
import com.example.minitasker.data.model.AuthResponse
import com.example.minitasker.data.model.CommentDto
import com.example.minitasker.data.model.CommentRequest
import com.example.minitasker.data.model.LoginRequest
import com.example.minitasker.data.model.PagedResponse
import com.example.minitasker.data.model.ProjectDto
import com.example.minitasker.data.model.ProjectRequest
import com.example.minitasker.data.model.RegisterRequest
import com.example.minitasker.data.model.StatsDto
import com.example.minitasker.data.model.TaskRequest
import com.example.minitasker.data.model.TaskResponseDto
import com.example.minitasker.data.model.TaskSummaryDto
import com.example.minitasker.data.model.UserDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface ApiService {

    // Auth
    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @GET("/api/auth/me")
    suspend fun currentUser(): UserDto

    // Projects
    @GET("/api/projects")
    suspend fun getProjects(
        @Query("name") name: String?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): PagedResponse<ProjectDto>

    @POST("/api/projects")
    suspend fun createProject(@Body request: ProjectRequest): ProjectDto

    @PUT("/api/projects/{id}")
    suspend fun updateProject(@Path("id") id: Long, @Body request: ProjectRequest): ProjectDto

    @DELETE("/api/projects/{id}")
    suspend fun deleteProject(@Path("id") id: Long)

    // Tasks
    @GET("/api/projects/{projectId}/tasks")
    suspend fun getTasks(
        @Path("projectId") projectId: Long,
        @Query("status") status: String?,
        @Query("priority") priority: String?,
        @Query("assigneeId") assigneeId: Long?,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): PagedResponse<TaskSummaryDto>

    @POST("/api/projects/{projectId}/tasks")
    suspend fun createTask(@Path("projectId") projectId: Long, @Body request: TaskRequest): TaskResponseDto

    @GET("/api/tasks/{taskId}")
    suspend fun getTask(@Path("taskId") taskId: Long): TaskResponseDto

    @PUT("/api/tasks/{taskId}")
    suspend fun updateTask(@Path("taskId") taskId: Long, @Body request: TaskRequest): TaskResponseDto

    @DELETE("/api/tasks/{taskId}")
    suspend fun deleteTask(@Path("taskId") taskId: Long)

    @POST("/api/projects/{projectId}/tasks/{taskId}/take")
    suspend fun takeTask(
        @Path("projectId") projectId: Long,
        @Path("taskId") taskId: Long
    ): TaskResponseDto

    // Comments
    @GET("/api/tasks/{taskId}/comments")
    suspend fun getComments(@Path("taskId") taskId: Long): List<CommentDto>

    @POST("/api/tasks/{taskId}/comments")
    suspend fun addComment(@Path("taskId") taskId: Long, @Body request: CommentRequest): CommentDto

    // Attachments
    @GET("/api/tasks/{taskId}/attachments")
    suspend fun getAttachments(@Path("taskId") taskId: Long): List<AttachmentDto>

    @Multipart
    @POST("/api/tasks/{taskId}/attachments")
    suspend fun uploadAttachment(
        @Path("taskId") taskId: Long,
        @Part file: MultipartBody.Part
    ): AttachmentDto

    @Streaming
    @GET
    suspend fun downloadAttachment(@Url url: String): retrofit2.Response<ResponseBody>

    // Stats
    @GET("/api/stats/tasks-by-status")
    suspend fun statsByStatus(@Query("projectId") projectId: Long): List<StatsDto>

    @GET("/api/stats/tasks-by-priority")
    suspend fun statsByPriority(@Query("projectId") projectId: Long): List<StatsDto>
}
