package com.example.bowlingmaster200.domain.calculator

import com.example.bowlingmaster200.domain.model.Frame
import com.example.bowlingmaster200.domain.model.FrameScore
import com.example.bowlingmaster200.domain.model.FrameType
import com.example.bowlingmaster200.domain.model.GameScore
import com.example.bowlingmaster200.domain.model.Roll

/**
 * スコア計算エンジン（Step 4-A: オープンフレームのみ）。
 * ストライク・スペア・10Fボーナスは未対応。該当フレーム以降は未確定（累計 null）。
 */
object BowlingScoreCalculator {

    fun calculateFrameScores(frames: List<Frame>): List<FrameScore> {
        require(frames.size == Frame.FRAME_COUNT) {
            "Expected ${Frame.FRAME_COUNT} frames, got ${frames.size}"
        }

        var runningTotal = 0
        var scoringStopped = false

        return frames.mapIndexed { index, frame ->
            val frameNumber = index + 1
            val frameType = classifyFrameType(frame, frameNumber)
            val framePins = resolveFramePins(frame, frameType)

            val cumulative = if (scoringStopped) {
                null
            } else {
                when (frameType) {
                    FrameType.OPEN -> {
                        val pins = framePins
                        if (pins == null) {
                            scoringStopped = true
                            null
                        } else {
                            runningTotal += pins
                            runningTotal
                        }
                    }
                    FrameType.STRIKE, FrameType.SPARE, FrameType.INCOMPLETE -> {
                        scoringStopped = true
                        null
                    }
                }
            }

            FrameScore(
                frameIndex = frameNumber,
                firstRoll = frame.firstRoll,
                secondRoll = frame.secondRoll,
                bonusRoll = frame.bonusRoll,
                frameType = frameType,
                framePins = framePins,
                cumulativeScore = cumulative,
            )
        }
    }

    fun calculateTotalScore(frames: List<Frame>): Int? {
        return calculateGameScore(frames).totalScore
    }

    fun calculateGameScore(frames: List<Frame>): GameScore {
        val frameScores = calculateFrameScores(frames)
        val complete = frameScores.all { it.cumulativeScore != null }
        return GameScore(
            frameScores = frameScores,
            totalScore = if (complete) frameScores.last().cumulativeScore else null,
            isComplete = complete,
        )
    }

    private fun classifyFrameType(frame: Frame, frameNumber: Int): FrameType {
        val first = frame.firstRoll ?: return FrameType.INCOMPLETE

        if (frameNumber < Frame.FRAME_COUNT) {
            if (first == Roll.MAX_PINS) return FrameType.STRIKE
            val second = frame.secondRoll ?: return FrameType.INCOMPLETE
            return if (first + second == Roll.FRAME_PINS) FrameType.SPARE else FrameType.OPEN
        }

        val second = frame.secondRoll ?: return FrameType.INCOMPLETE
        if (first == Roll.MAX_PINS) return FrameType.STRIKE
        return if (first + second == Roll.FRAME_PINS) FrameType.SPARE else FrameType.OPEN
    }

    private fun resolveFramePins(frame: Frame, frameType: FrameType): Int? {
        if (frame.firstRoll == null) return null

        return when (frameType) {
            FrameType.OPEN -> {
                val second = frame.secondRoll ?: return null
                frame.firstRoll + second
            }
            FrameType.STRIKE, FrameType.SPARE -> Roll.FRAME_PINS
            FrameType.INCOMPLETE -> null
        }
    }
}
