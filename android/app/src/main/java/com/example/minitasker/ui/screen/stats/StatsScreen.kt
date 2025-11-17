package com.example.minitasker.ui.screen.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.example.minitasker.data.model.TaskPriority
import com.example.minitasker.data.model.TaskStatus
import com.example.minitasker.data.model.StatsDto

@Composable
fun StatsScreen(projectId: Long, projectName: String, viewModel: StatsViewModel) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.load(projectId)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Статистика по проекту $projectName", style = MaterialTheme.typography.headlineSmall)
        AndroidView(modifier = Modifier
            .padding(vertical = 12.dp)
            .fillMaxWidth()
            .height(220.dp), factory = { context ->
            PieChart(context).apply {
                description.isEnabled = false
            }
        }, update = { chart ->
            val entries = state.statusStats.map { PieEntry(it.value.toFloat(), it.key) }
            val dataSet = PieDataSet(entries, "Статусы")
            chart.data = PieData(dataSet)
            chart.invalidate()
        })
        StatsListSection(title = "По статусам", stats = state.statusStats, labelProvider = { formatStatusLabel(it) })
        Spacer(modifier = Modifier.height(16.dp))
        AndroidView(modifier = Modifier
            .padding(vertical = 12.dp)
            .fillMaxWidth()
            .height(220.dp), factory = { context ->
            PieChart(context).apply { description.isEnabled = false }
        }, update = { chart ->
            val entries = state.priorityStats.map { PieEntry(it.value.toFloat(), it.key) }
            val dataSet = PieDataSet(entries, "Приоритеты")
            chart.data = PieData(dataSet)
            chart.invalidate()
        })
        StatsListSection(title = "По приоритетам", stats = state.priorityStats, labelProvider = { formatPriorityLabel(it) })
    }
}

@Composable
private fun StatsListSection(title: String, stats: List<StatsDto>, labelProvider: (String) -> String) {
    Column {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (stats.isEmpty()) {
            Text(text = "Нет данных")
        } else {
            stats.forEach { stat ->
                Text(text = "${labelProvider(stat.key)}: ${stat.value}")
            }
        }
    }
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
