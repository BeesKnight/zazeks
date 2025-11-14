package com.example.minitasker.data.model

data class ProjectRequest(
    val name: String,
    val description: String?
)

data class ProjectDto(
    val id: Long,
    val name: String,
    val description: String?,
    val ownerId: Long,
    val ownerName: String,
    val createdAt: String
)

data class PagedResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int
)
