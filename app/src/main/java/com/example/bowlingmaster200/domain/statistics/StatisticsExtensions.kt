package com.example.bowlingmaster200.domain.statistics

import com.example.bowlingmaster200.domain.model.SavedGameDetail

/** 統計計算用の拡張。将来グラフ用の集計もここへ追加可能。 */
fun List<SavedGameDetail>.toStatisticsSummary(): StatisticsSummary {
    return BowlingStatisticsCalculator.calculateSummary(this)
}

fun List<SavedGameDetail>.completedTotalScores(): List<Int> {
    return mapNotNull { it.gameScore.totalScore }
}

fun List<SavedGameDetail>.sortedByPlayedAtDesc(): List<SavedGameDetail> {
    return sortedByDescending { it.metadata.playedAt }
}

fun StatisticsSummary.formatAverage(): String {
    return averageScore?.let { "%.1f".format(it) } ?: "-"
}

fun StatisticsSummary.formatRate(rate: Double?): String {
    return rate?.let { "%.1f%%".format(it) } ?: "-"
}
