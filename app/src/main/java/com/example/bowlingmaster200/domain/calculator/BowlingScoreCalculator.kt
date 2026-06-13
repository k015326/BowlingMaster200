package com.example.bowlingmaster200.domain.calculator

import com.example.bowlingmaster200.domain.model.Frame
import com.example.bowlingmaster200.domain.model.FrameScore
import com.example.bowlingmaster200.domain.model.FrameType
import com.example.bowlingmaster200.domain.model.GameScore
import com.example.bowlingmaster200.domain.model.Roll

/**
 * スコア計算エンジン（Step 4-C: オープン + スペア + ストライク + 10F特殊処理）。
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
                    FrameType.SPARE -> {
                        val bonusRoll = if (index >= Frame.LAST_FRAME_INDEX) {
                            tenthFrameSpareBonusRoll(frame)
                        } else {
                            nextRolls(frames, index, 1)?.firstOrNull()
                        }
                        if (bonusRoll == null) {
                            scoringStopped = true
                            null
                        } else {
                            runningTotal += Roll.FRAME_PINS + bonusRoll
                            runningTotal
                        }
                    }
                    FrameType.STRIKE -> {
                        val frameScore = if (index >= Frame.LAST_FRAME_INDEX) {
                            tenthFrameStrikeScore(frame)
                        } else {
                            nextRolls(frames, index, 2)?.let { Roll.FRAME_PINS + it.sum() }
                        }
                        if (frameScore == null) {
                            scoringStopped = true
                            null
                        } else {
                            runningTotal += frameScore
                            runningTotal
                        }
                    }
                    FrameType.INCOMPLETE -> {
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

    private fun nextRolls(frames: List<Frame>, afterFrameIndex: Int, count: Int): List<Int>? {
        val rolls = mutableListOf<Int>()
        var frameIndex = afterFrameIndex + 1

        while (rolls.size < count && frameIndex < frames.size) {
            val frame = frames[frameIndex]
            val isTenthFrame = frameIndex == Frame.LAST_FRAME_INDEX

            val first = frame.firstRoll ?: return null
            rolls.add(first)
            if (rolls.size >= count) return rolls

            if (!isTenthFrame && first == Roll.MAX_PINS) {
                frameIndex++
                continue
            }

            val second = frame.secondRoll ?: return null
            rolls.add(second)
            if (rolls.size >= count) return rolls

            if (isTenthFrame) {
                frame.bonusRoll?.let { rolls.add(it) }
            }
            frameIndex++
        }

        return if (rolls.size >= count) rolls else null
    }

    private fun tenthFrameSpareBonusRoll(frame: Frame): Int? {
        val first = frame.firstRoll ?: return null
        val second = frame.secondRoll ?: return null
        if (first + second != Roll.FRAME_PINS) return null
        return frame.bonusRoll
    }

    private fun tenthFrameStrikeScore(frame: Frame): Int? {
        val first = frame.firstRoll ?: return null
        if (first != Roll.MAX_PINS) return null

        val second = frame.secondRoll ?: return null
        val bonus = frame.bonusRoll ?: return null
        return Roll.FRAME_PINS + second + bonus
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
