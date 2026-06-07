package com.example.bowlingmaster200.ui.stats

import com.example.bowlingmaster200.domain.statistics.StatisticsSummary

data class StatsUiState(
    val summary: StatisticsSummary = StatisticsSummary.empty(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
