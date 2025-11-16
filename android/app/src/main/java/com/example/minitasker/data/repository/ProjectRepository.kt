package com.example.minitasker.data.repository

import com.example.minitasker.data.api.ApiService
import com.example.minitasker.data.model.PagedResponse
import com.example.minitasker.data.model.ProjectDto
import com.example.minitasker.data.model.ProjectRequest

class ProjectRepository(private val apiService: ApiService) {

    suspend fun loadProjects(filter: String?, page: Int, size: Int): PagedResponse<ProjectDto> {
        return apiService.getProjects(filter, page, size)
    }

    suspend fun createProject(name: String, description: String?): ProjectDto {
        return apiService.createProject(ProjectRequest(name, description))
    }

    suspend fun updateProject(id: Long, name: String, description: String?): ProjectDto {
        return apiService.updateProject(id, ProjectRequest(name, description))
    }

    suspend fun deleteProject(id: Long) {
        apiService.deleteProject(id)
    }
}
