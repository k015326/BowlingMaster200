package com.example.bowlingmaster200.domain.calculator

import com.example.bowlingmaster200.domain.model.Frame
import com.example.bowlingmaster200.domain.model.FrameScore
import com.example.bowlingmaster200.domain.model.FrameType
import com.example.bowlingmaster200.domain.model.GameScore
import com.example.bowlingmaster200.domain.model.Roll

/**
 * 正式ボウリングルールに基づくスコア計算エンジン。
 * 純粋 Kotlin — Android / Compose 依存なし。
 */
object BowlingScoreCalculator {

    fun calculateFrameScores(frames: List<Frame>): List<FrameScore> {
        require(frames.size == Frame.FRAME_COUNT) {
            "Expected ${Frame.FRAME_COUNT} frames, got ${frames.size}"
        }

        val rolls = RollExpander.expand(frames)
        var rollIndex = 0
        var runningTotal = 0

        return frames.mapIndexed { index, frame ->
            val frameNumber = index + 1
            val frameType = resolveFrameType(frame, frameNumber)
            val framePins = resolveFramePins(frame, frameNumber)

            val cumulative = when {
                frameNumber < Frame.FRAME_COUNT -> {
                    scoreRegularFrame(rolls, rollIndex, runningTotal)?.let { (total, consumed) ->
                        rollIndex += consumed
                        runningTotal = total
                        total
                    }
                }
                else -> {
                    scoreTenthFrame(frame, rolls, rollIndex, runningTotal)?.let { (total, consumed) ->
                        rollIndex += consumed
                        runningTotal = total
                        total
                    }
                }
            }

            FrameScore(
                frameIndex = frameNumber,
                firstRoll = frame.firstRoll,
                secondRoll = frame.secondRoll,
                bonusRoll = frame.bonusRoll,
                frameType = if (cumulative != null) frameType else FrameType.INCOMPLETE,
                framePins = framePins,
                cumulativeScore = cumulative,
            )
        }
    }

    fun calculateTotalScore(frames: List<Frame>): Int? {
        val frameScores = calculateFrameScores(frames)
        if (!isGameComplete(frameScores)) return null
        return frameScores.last().cumulativeScore
    }

    fun calculateGameScore(frames: List<Frame>): GameScore {
        val frameScores = calculateFrameScores(frames)
        val complete = isGameComplete(frameScores)
        return GameScore(
            frameScores = frameScores,
            totalScore = if (complete) frameScores.last().cumulativeScore else null,
            isComplete = complete,
        )
    }

    private fun scoreRegularFrame(
        rolls: List<Int>,
        rollIndex: Int,
        runningTotal: Int,
    ): Pair<Int, Int>? {
        if (rollIndex >= rolls.size) return null

        return if (rolls[rollIndex] == Roll.MAX_PINS) {
            if (rollIndex + 2 >= rolls.size) return null
            val total = runningTotal + Roll.MAX_PINS + rolls[rollIndex + 1] + rolls[rollIndex + 2]
            total to 1
        } else {
            if (rollIndex + 1 >= rolls.size) return null
            val first = rolls[rollIndex]
            val second = rolls[rollIndex + 1]
            val frameSum = first + second
            if (frameSum == Roll.FRAME_PINS) {
                if (rollIndex + 2 >= rolls.size) return null
                val total = runningTotal + Roll.FRAME_PINS + rolls[rollIndex + 2]
                total to 2
            } else {
                val total = runningTotal + frameSum
                total to 2
            }
        }
    }

    private fun scoreTenthFrame(
        frame: Frame,
        rolls: List<Int>,
        rollIndex: Int,
        runningTotal: Int,
    ): Pair<Int, Int>? {
        if (!TenthFrameRules.isComplete(frame)) return null
        val rollCount = TenthFrameRules.completedRollCount(frame)
        if (rollIndex + rollCount > rolls.size) return null
        val frameSum = rolls.subList(rollIndex, rollIndex + rollCount).sum()
        return (runningTotal + frameSum) to rollCount
    }

    private fun isGameComplete(frameScores: List<FrameScore>): Boolean {
        return frameScores.all { it.cumulativeScore != null }
    }

    private fun resolveFrameType(frame: Frame, frameNumber: Int): FrameType {
        val first = frame.firstRoll ?: return FrameType.INCOMPLETE

        if (frameNumber < Frame.FRAME_COUNT) {
            if (first == Roll.MAX_PINS) return FrameType.STRIKE
            val second = frame.secondRoll ?: return FrameType.INCOMPLETE
            return when {
                first + second == Roll.FRAME_PINS -> FrameType.SPARE
                else -> FrameType.OPEN
            }
        }

        if (!TenthFrameRules.isComplete(frame)) return FrameType.INCOMPLETE
        if (first == Roll.MAX_PINS) return FrameType.STRIKE
        val second = frame.secondRoll!!
        return if (first + second == Roll.FRAME_PINS) FrameType.SPARE else FrameType.OPEN
    }

    private fun resolveFramePins(frame: Frame, frameNumber: Int): Int? {
        val first = frame.firstRoll ?: return null

        if (frameNumber < Frame.FRAME_COUNT) {
            if (first == Roll.MAX_PINS) return Roll.FRAME_PINS
            val second = frame.secondRoll ?: return null
            return first + second
        }

        if (!TenthFrameRules.isComplete(frame)) return null
        return listOfNotNull(frame.firstRoll, frame.secondRoll, frame.bonusRoll).sum()
    }
}

/** フレーム入力をフラットな投球列に展開する。 */
internal object RollExpander {

    fun expand(frames: List<Frame>): List<Int> {
        val rolls = mutableListOf<Int>()
        frames.forEachIndexed { index, frame ->
            val frameNumber = index + 1
            val first = frame.firstRoll ?: return@forEachIndexed
            rolls.add(first)

            if (frameNumber < Frame.FRAME_COUNT) {
                if (first != Roll.MAX_PINS) {
                    frame.secondRoll?.let { rolls.add(it) }
                }
            } else {
                frame.secondRoll?.let { rolls.add(it) }
                frame.bonusRoll?.let { rolls.add(it) }
            }
        }
        return rolls
    }
}

/** 10フレーム目の投球数・完了判定。 */
internal object TenthFrameRules {

    fun isComplete(frame: Frame): Boolean {
        val first = frame.firstRoll ?: return false
        if (first == Roll.MAX_PINS) {
            return frame.secondRoll != null && frame.bonusRoll != null
        }
        val second = frame.secondRoll ?: return false
        if (first + second == Roll.FRAME_PINS) {
            return frame.bonusRoll != null
        }
        return true
    }

    fun completedRollCount(frame: Frame): Int {
        val first = frame.firstRoll ?: return 0
        if (first == Roll.MAX_PINS) return 3
        val second = frame.secondRoll ?: return 1
        return if (first + second == Roll.FRAME_PINS) 3 else 2
    }
}
