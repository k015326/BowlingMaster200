package com.example.bowlingmaster200.domain.statistics

import com.example.bowlingmaster200.domain.model.FrameType
import com.example.bowlingmaster200.domain.model.SavedGameDetail

object BowlingStatisticsCalculator {

    fun calculateSummary(games: List<SavedGameDetail>): StatisticsSummary {
        if (games.isEmpty()) {
            return StatisticsSummary.empty()
        }

        val totalScores = games.mapNotNull { it.gameScore.totalScore }
        val gamesCount = games.size

        val averageScore = totalScores.takeIf { it.isNotEmpty() }?.average()
        val highGame = totalScores.maxOrNull()
        val lowGame = totalScores.minOrNull()

        var strikeCount = 0
        var spareCount = 0
        var openCount = 0
        var frameCount = 0

        games.forEach { game ->
            game.gameScore.frameScores.forEach { frame ->
                when (frame.frameType) {
                    FrameType.STRIKE -> {
                        strikeCount++
                        frameCount++
                    }
                    FrameType.SPARE -> {
                        spareCount++
                        frameCount++
                    }
                    FrameType.OPEN -> {
                        openCount++
                        frameCount++
                    }
                    FrameType.INCOMPLETE -> Unit
                }
            }
        }

        return StatisticsSummary(
            gamesCount = gamesCount,
            averageScore = averageScore,
            highGame = highGame,
            lowGame = lowGame,
            strikeRate = frameCount.toPercent(strikeCount),
            spareRate = frameCount.toPercent(spareCount),
            openRate = frameCount.toPercent(openCount),
        )
    }

    private fun Int.toPercent(count: Int): Double? {
        if (this <= 0) return null
        return count * 100.0 / this
    }
}
