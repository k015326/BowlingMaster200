package com.example.bowlingmaster200.ui.stats

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bowlingmaster200.domain.statistics.StatisticsSummary
import com.example.bowlingmaster200.domain.statistics.formatAverage
import com.example.bowlingmaster200.domain.statistics.formatRate

@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = viewModel(
        factory = StatsViewModel.factory(
            LocalContext.current.applicationContext as Application,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = "Statistics",
            style = MaterialTheme.typography.titleLarge,
        )

        when {
            uiState.isLoading -> {
                Text(
                    text = "Loading...",
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            uiState.errorMessage != null -> {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            !uiState.summary.hasData -> {
                Text(
                    text = "保存済みゲームがありません",
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            else -> {
                StatsSummaryContent(summary = uiState.summary)
            }
        }
    }
}

@Composable
private fun StatsSummaryContent(summary: StatisticsSummary) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        StatRow(label = "Games Count", value = summary.gamesCount.toString())
        StatRow(label = "AVG", value = summary.formatAverage())
        StatRow(label = "High Game", value = summary.highGame?.toString() ?: "-")
        StatRow(label = "Low Game", value = summary.lowGame?.toString() ?: "-")
        StatRow(label = "Strike%", value = summary.formatRate(summary.strikeRate))
        StatRow(label = "Spare%", value = summary.formatRate(summary.spareRate))
        StatRow(label = "Open%", value = summary.formatRate(summary.openRate))
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}
