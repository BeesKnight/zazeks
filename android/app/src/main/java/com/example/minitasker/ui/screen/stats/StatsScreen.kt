package com.example.minitasker.ui.screen.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import com.example.minitasker.data.model.StatsDto
import com.example.minitasker.data.model.TaskPriority
import com.example.minitasker.data.model.TaskStatus
import com.example.minitasker.ui.theme.MiniTaskerTheme
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(projectId: Long, projectName: String, viewModel: StatsViewModel) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.load(projectId)
    }

    StatsScreenContent(projectName = projectName, state = state)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsScreenContent(projectName: String, state: StatsUiState) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Статистика по проекту $projectName") })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StatsCard(
                    title = "Статусы",
                    data = state.statusStats,
                    labelProvider = { formatStatusLabel(it) },
                    colorProvider = { key -> statusColorForKey(key, MaterialTheme.colorScheme) },
                    chartLabel = "Статусы"
                )
            }
            item {
                StatsCard(
                    title = "Приоритеты",
                    data = state.priorityStats,
                    labelProvider = { formatPriorityLabel(it) },
                    colorProvider = { key -> priorityColorForKey(key, MaterialTheme.colorScheme) },
                    chartLabel = "Приоритеты"
                )
            }
            if (state.loading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            state.error?.let { message ->
                item {
                    Text(text = message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun StatsCard(
    title: String,
    data: List<StatsDto>,
    labelProvider: (String) -> String,
    colorProvider: (String) -> Color,
    chartLabel: String,
) {
    ElevatedCard(shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (data.isEmpty()) {
                Text(text = "Нет данных", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                PieChartView(data = data, colorProvider = colorProvider, labelProvider = labelProvider, label = chartLabel)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.forEach { stat ->
                        val color = colorProvider(stat.key)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(color = color, shape = CircleShape)
                            )
                            Text(text = "${labelProvider(stat.key)}: ${stat.value}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PieChartView(
    data: List<StatsDto>,
    colorProvider: (String) -> Color,
    labelProvider: (String) -> String,
    label: String
) {
    val entries = data.map { PieEntry(it.value.toFloat(), labelProvider(it.key)) }
    val colors = data.map { colorProvider(it.key).toArgb() }
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        factory = { context ->
            PieChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setUsePercentValues(false)
            }
        },
        update = { chart ->
            val dataSet = PieDataSet(entries, label)
            dataSet.colors = colors
            val pieData = PieData(dataSet)
            pieData.setDrawValues(false)
            chart.data = pieData
            chart.invalidate()
        }
    )
}

private fun formatStatusLabel(key: String): String = when (key) {
    TaskStatus.TODO.name -> "Сделать"
    TaskStatus.IN_PROGRESS.name -> "В работе"
    TaskStatus.DONE.name -> "Готово"
    else -> key
}

private fun formatPriorityLabel(key: String): String = when (key) {
    TaskPriority.HIGH.name -> "Высокий"
    TaskPriority.MEDIUM.name -> "Средний"
    TaskPriority.LOW.name -> "Низкий"
    else -> key
}

private fun statusColorForKey(key: String, scheme: ColorScheme): Color {
    return when (key) {
        TaskStatus.TODO.name -> scheme.surfaceVariant
        TaskStatus.IN_PROGRESS.name -> scheme.secondaryContainer
        TaskStatus.DONE.name -> scheme.tertiaryContainer
        else -> scheme.primaryContainer
    }
}

private fun priorityColorForKey(key: String, scheme: ColorScheme): Color {
    return when (key) {
        TaskPriority.LOW.name -> scheme.surfaceVariant
        TaskPriority.MEDIUM.name -> scheme.primaryContainer
        TaskPriority.HIGH.name -> scheme.errorContainer
        else -> scheme.secondaryContainer
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsScreenPreview() {
    MiniTaskerTheme {
        val statusStats = listOf(
            StatsDto(TaskStatus.TODO.name, 5),
            StatsDto(TaskStatus.IN_PROGRESS.name, 3),
            StatsDto(TaskStatus.DONE.name, 8)
        )
        val priorityStats = listOf(
            StatsDto(TaskPriority.HIGH.name, 4),
            StatsDto(TaskPriority.MEDIUM.name, 6),
            StatsDto(TaskPriority.LOW.name, 2)
        )
        StatsScreenContent(
            projectName = "Проект X",
            state = StatsUiState(statusStats = statusStats, priorityStats = priorityStats)
        )
    }
}
