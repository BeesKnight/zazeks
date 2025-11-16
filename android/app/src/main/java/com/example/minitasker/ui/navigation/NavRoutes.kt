package com.example.minitasker.ui.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Projects : Screen("projects")
    object Tasks : Screen("tasks/{projectId}/{projectName}") {
        fun create(projectId: Long, projectName: String) = "tasks/$projectId/${projectName}"
    }
    object TaskDetail : Screen("taskDetail/{taskId}") {
        fun create(taskId: Long) = "taskDetail/$taskId"
    }
    object Stats : Screen("stats/{projectId}/{projectName}") {
        fun create(projectId: Long, projectName: String) = "stats/$projectId/${projectName}"
    }
}
