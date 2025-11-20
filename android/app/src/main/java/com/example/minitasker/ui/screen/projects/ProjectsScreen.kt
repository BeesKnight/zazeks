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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.minitasker.data.model.ProjectDto
import com.example.minitasker.ui.components.LabeledTextField
import com.example.minitasker.ui.theme.MiniTaskerTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.error) {
        state.error?.let { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
            viewModel.clearError()
        }
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
        onLogout = { viewModel.logout(onLoggedOut) },
        onDeleteProject = { viewModel.deleteProject(it) },
        onRefresh = { viewModel.refresh() },
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
private fun ProjectsScreenContent(
    state: ProjectsUiState,
    onProjectSelected: (ProjectDto) -> Unit,
    onShowStats: (ProjectDto) -> Unit,
    onCreateProject: () -> Unit,
    onLogout: () -> Unit,
    onDeleteProject: (Long) -> Unit,
    onRefresh: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val refreshState = rememberPullRefreshState(refreshing = state.loading, onRefresh = onRefresh)
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
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(refreshState)
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
                    ProjectCard(
                        project = project,
                        onProjectSelected = onProjectSelected,
                        onShowStats = onShowStats,
                        onDeleteProject = onDeleteProject
                    )
                }
            }
            PullRefreshIndicator(refreshing = state.loading, state = refreshState, modifier = Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun ProjectCard(
    project: ProjectDto,
    onProjectSelected: (ProjectDto) -> Unit,
    onShowStats: (ProjectDto) -> Unit,
    onDeleteProject: (Long) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteProject(project.id)
                    showDeleteDialog = false
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
            },
            title = { Text("Удалить проект?") },
            text = { Text("Удалить проект вместе со всеми задачами?") }
        )
    }

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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                }
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
