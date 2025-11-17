package com.example.minitasker.ui.screen.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.minitasker.data.model.ProjectDto
import com.example.minitasker.ui.components.LabeledTextField
import com.example.minitasker.ui.theme.MiniTaskerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: ProjectsViewModel,
    onProjectSelected: (ProjectDto) -> Unit,
    onShowStats: (ProjectDto) -> Unit,
    onLoggedOut: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var projectName by remember { mutableStateOf("") }
    var projectDescription by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    if (state.showCreateDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleCreateDialog(false) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createProject(projectName, projectDescription)
                    projectName = ""
                    projectDescription = ""
                }) { Text("Создать") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.toggleCreateDialog(false) }) { Text("Отмена") }
            },
            title = { Text("Новый проект") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledTextField(value = projectName, onValueChange = { projectName = it }, label = "Название")
                    LabeledTextField(value = projectDescription, onValueChange = { projectDescription = it }, label = "Описание")
                }
            }
        )
    }

    ProjectsScreenContent(
        state = state,
        onProjectSelected = onProjectSelected,
        onShowStats = onShowStats,
        onCreateProject = { viewModel.toggleCreateDialog(true) },
        onLogout = { viewModel.logout(onLoggedOut) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectsScreenContent(
    state: ProjectsUiState,
    onProjectSelected: (ProjectDto) -> Unit,
    onShowStats: (ProjectDto) -> Unit,
    onCreateProject: () -> Unit,
    onLogout: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Проекты MiniTasker") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(imageVector = Icons.Default.Logout, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onCreateProject, text = { Text("Новый проект") }, icon = {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                if (state.projects.isEmpty() && !state.loading) {
                    item {
                        Text(
                            text = "Проекты не найдены",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(state.projects, key = { it.id }) { project ->
                    ProjectCard(project = project, onProjectSelected = onProjectSelected, onShowStats = onShowStats)
                }
                state.error?.let { errorMessage ->
                    item {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun ProjectCard(project: ProjectDto, onProjectSelected: (ProjectDto) -> Unit, onShowStats: (ProjectDto) -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        onClick = { onProjectSelected(project) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = project.description.takeUnless { it.isNullOrBlank() } ?: "Без описания",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Владелец: ${project.ownerName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onShowStats(project) }) {
                    Icon(imageVector = Icons.Default.PieChart, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Статистика")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProjectsScreenPreview() {
    MiniTaskerTheme {
        val sampleProjects = listOf(
            ProjectDto(1, "Новый маркетинг", "Запуск летней кампании", 1, "Анна", ""),
            ProjectDto(2, "Мобильное приложение", "Версия 2.0", 2, "Иван", "")
        )
        ProjectsScreenContent(
            state = ProjectsUiState(projects = sampleProjects),
            onProjectSelected = {},
            onShowStats = {},
            onCreateProject = {},
            onLogout = {}
        )
    }
}
