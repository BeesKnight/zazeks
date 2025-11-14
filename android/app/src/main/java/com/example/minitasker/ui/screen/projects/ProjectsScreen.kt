package com.example.minitasker.ui.screen.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.Add
import androidx.compose.material3.icons.filled.Logout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.minitasker.data.model.ProjectDto
import com.example.minitasker.ui.components.LabeledTextField

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
                Column {
                    LabeledTextField(value = projectName, onValueChange = { projectName = it }, label = "Название")
                    LabeledTextField(value = projectDescription, onValueChange = { projectDescription = it }, label = "Описание")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Проекты MiniTasker") },
                actions = {
                    IconButton(onClick = { viewModel.logout(onLoggedOut) }) {
                        Icon(imageVector = Icons.Default.Logout, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.toggleCreateDialog(true) }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (state.loading) {
                CircularProgressIndicator()
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f, fill = false)) {
                items(state.projects) { project ->
                    ProjectCard(project = project, onProjectSelected = onProjectSelected, onShowStats = onShowStats)
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(project: ProjectDto, onProjectSelected: (ProjectDto) -> Unit, onShowStats: (ProjectDto) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProjectSelected(project) }
            .padding(16.dp)
    ) {
        Text(text = project.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = project.description ?: "Без описания", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Владелец: ${project.ownerName}")
            TextButton(onClick = { onShowStats(project) }) { Text("Статистика") }
        }
    }
}
