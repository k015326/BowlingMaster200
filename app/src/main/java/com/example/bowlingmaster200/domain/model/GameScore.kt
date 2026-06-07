package com.example.bowlingmaster200.domain.model

/**
 * 1ゲーム分のスコア計算結果。
 */
data class GameScore(
    val frameScores: List<FrameScore>,
    val totalScore: Int?,
    val isComplete: Boolean,
) {
    init {
        require(frameScores.size == Frame.FRAME_COUNT) {
            "frameScores must contain ${Frame.FRAME_COUNT} frames, got ${frameScores.size}"
        }
    }

    companion object {
        fun empty(): GameScore {
            val frames = (1..Frame.FRAME_COUNT).map { index ->
                FrameScore(
                    frameIndex = index,
                    firstRoll = null,
                    secondRoll = null,
                    bonusRoll = null,
                    frameType = FrameType.INCOMPLETE,
                    framePins = null,
                    cumulativeScore = null,
                )
            }
            return GameScore(
                frameScores = frames,
                totalScore = null,
                isComplete = false,
            )
        }
    }
}
