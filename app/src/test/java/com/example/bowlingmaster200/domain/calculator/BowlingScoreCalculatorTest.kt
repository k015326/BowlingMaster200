package com.example.bowlingmaster200.domain.calculator

import com.example.bowlingmaster200.domain.model.Frame
import com.example.bowlingmaster200.domain.model.FrameType
import com.example.bowlingmaster200.domain.model.Roll
import com.example.bowlingmaster200.domain.validator.FrameValidator
import com.example.bowlingmaster200.domain.validator.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BowlingScoreCalculatorTest {

    @Test
    fun openFrames_cumulativeScore_example() {
        val frames = buildFrames(
            Frame(firstRoll = 9, secondRoll = 0),
            Frame(firstRoll = 8, secondRoll = 1),
            Frame(firstRoll = 7, secondRoll = 2),
        )
        val scores = BowlingScoreCalculator.calculateFrameScores(frames)

        assertEquals(9, scores[0].framePins)
        assertEquals(9, scores[0].cumulativeScore)
        assertEquals(FrameType.OPEN, scores[0].frameType)

        assertEquals(9, scores[1].framePins)
        assertEquals(18, scores[1].cumulativeScore)
        assertEquals(FrameType.OPEN, scores[1].frameType)

        assertEquals(9, scores[2].framePins)
        assertEquals(27, scores[2].cumulativeScore)
        assertEquals(FrameType.OPEN, scores[2].frameType)
    }

    @Test
    fun allOpenFrames_totalScore() {
        val frames = List(Frame.FRAME_COUNT) { Frame(firstRoll = 9, secondRoll = 0) }
        assertEquals(90, BowlingScoreCalculator.calculateTotalScore(frames))
    }

    @Test
    fun allGutter_scoresZero() {
        val frames = List(Frame.FRAME_COUNT) { Frame(firstRoll = 0, secondRoll = 0) }
        assertEquals(0, BowlingScoreCalculator.calculateTotalScore(frames))
    }

    @Test
    fun strikeFrame_stopsScoring() {
        val frames = buildFrames(
            Frame(firstRoll = 9, secondRoll = 0),
            Frame(firstRoll = Roll.MAX_PINS),
            Frame(firstRoll = 8, secondRoll = 1),
        )
        val scores = BowlingScoreCalculator.calculateFrameScores(frames)

        assertEquals(9, scores[0].cumulativeScore)
        assertEquals(FrameType.STRIKE, scores[1].frameType)
        assertNull(scores[1].cumulativeScore)
        assertNull(scores[2].cumulativeScore)
        assertNull(BowlingScoreCalculator.calculateTotalScore(frames))
    }

    @Test
    fun spareFrame_stopsScoring() {
        val frames = buildFrames(
            Frame(firstRoll = 9, secondRoll = 0),
            Frame(firstRoll = 7, secondRoll = 3),
            Frame(firstRoll = 8, secondRoll = 1),
        )
        val scores = BowlingScoreCalculator.calculateFrameScores(frames)

        assertEquals(9, scores[0].cumulativeScore)
        assertEquals(FrameType.SPARE, scores[1].frameType)
        assertNull(scores[1].cumulativeScore)
        assertNull(scores[2].cumulativeScore)
    }

    @Test
    fun incompleteOpenFrame_stopsScoring() {
        val frames = buildFrames(
            Frame(firstRoll = 9, secondRoll = 0),
            Frame(firstRoll = 8),
        )
        val scores = BowlingScoreCalculator.calculateFrameScores(frames)

        assertEquals(9, scores[0].cumulativeScore)
        assertEquals(FrameType.INCOMPLETE, scores[1].frameType)
        assertNull(scores[1].cumulativeScore)
        assertNull(BowlingScoreCalculator.calculateTotalScore(frames))
    }

    @Test
    fun validator_rejectsSecondRollAfterStrike() {
        val result = FrameValidator.validateFrame(
            Frame(firstRoll = 10, secondRoll = 5),
            frameIndex = 0,
        )
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun validator_rejectsFrameSumOverTen() {
        val result = FrameValidator.validateFrame(
            Frame(firstRoll = 6, secondRoll = 5),
            frameIndex = 0,
        )
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun validator_rejectsBonusOnOpenTenthFrame() {
        val result = FrameValidator.validateFrame(
            Frame(firstRoll = 7, secondRoll = 2, bonusRoll = 1),
            frameIndex = Frame.LAST_FRAME_INDEX,
        )
        assertTrue(result is ValidationResult.Invalid)
    }

    private fun buildFrames(vararg partial: Frame): List<Frame> {
        val list = partial.toMutableList()
        while (list.size < Frame.FRAME_COUNT) {
            list.add(Frame())
        }
        return list
    }
}
