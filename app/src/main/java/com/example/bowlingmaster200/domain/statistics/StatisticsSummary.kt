package com.example.bowlingmaster200.domain.statistics

/**
 * 保存済みゲーム群の統計サマリー。
 * 将来のグラフ画面でも同一モデルを再利用可能。
 */
data class StatisticsSummary(
    val gamesCount: Int = 0,
    val averageScore: Double? = null,
    val highGame: Int? = null,
    val lowGame: Int? = null,
    val strikeRate: Double? = null,
    val spareRate: Double? = null,
    val openRate: Double? = null,
) {
    val hasData: Boolean get() = gamesCount > 0

    companion object {
        fun empty(): StatisticsSummary = StatisticsSummary()
    }
}
