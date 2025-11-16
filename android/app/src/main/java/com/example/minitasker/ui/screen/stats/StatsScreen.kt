package com.example.minitasker.ui.screen.stats

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

@Composable
fun StatsScreen(projectId: Long, projectName: String, viewModel: StatsViewModel) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.load(projectId)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Статистика по проекту $projectName", style = MaterialTheme.typography.headlineSmall)
        AndroidView(factory = { context ->
            PieChart(context).apply {
                description.isEnabled = false
            }
        }, update = { chart ->
            val entries = state.statusStats.map { PieEntry(it.value.toFloat(), it.key) }
            val dataSet = PieDataSet(entries, "Статусы")
            chart.data = PieData(dataSet)
            chart.invalidate()
        })
        AndroidView(factory = { context ->
            PieChart(context).apply { description.isEnabled = false }
        }, update = { chart ->
            val entries = state.priorityStats.map { PieEntry(it.value.toFloat(), it.key) }
            val dataSet = PieDataSet(entries, "Приоритеты")
            chart.data = PieData(dataSet)
            chart.invalidate()
        })
    }
}
