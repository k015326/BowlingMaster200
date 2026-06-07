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
    fun perfectGame_scores300() {
        val frames = List(Frame.FRAME_COUNT) { Frame(firstRoll = Roll.MAX_PINS) }
        assertEquals(300, BowlingScoreCalculator.calculateTotalScore(frames))
    }

    @Test
    fun allSpares_withFiveOnFirst_scores150() {
        val frames = List(Frame.FRAME_COUNT) {
            Frame(firstRoll = 5, secondRoll = 5, bonusRoll = if (it == Frame.LAST_FRAME_INDEX) 5 else null)
        }
        assertEquals(150, BowlingScoreCalculator.calculateTotalScore(frames))
    }

    @Test
    fun allGutter_scoresZero() {
        val frames = List(Frame.FRAME_COUNT) { Frame(firstRoll = 0, secondRoll = 0) }
        assertEquals(0, BowlingScoreCalculator.calculateTotalScore(frames))
    }

    @Test
    fun openFrames_example() {
        // 7/ 8- = frame1: 7+3+8=18, frame2: 8 = 26
        val frames = buildFrames(
            Frame(firstRoll = 7, secondRoll = 3),
            Frame(firstRoll = 8, secondRoll = 0),
        )
        val scores = BowlingScoreCalculator.calculateFrameScores(frames)
        assertEquals(18, scores[0].cumulativeScore)
        assertEquals(26, scores[1].cumulativeScore)
        assertEquals(FrameType.SPARE, scores[0].frameType)
        assertEquals(FrameType.OPEN, scores[1].frameType)
    }

    @Test
    fun strike_followedBySpareAndOpen() {
        // X 7/ 8- = 20, 38, 46
        val frames = buildFrames(
            Frame(firstRoll = 10),
            Frame(firstRoll = 7, secondRoll = 3),
            Frame(firstRoll = 8, secondRoll = 0),
        )
        val scores = BowlingScoreCalculator.calculateFrameScores(frames)
        assertEquals(20, scores[0].cumulativeScore)
        assertEquals(38, scores[1].cumulativeScore)
        assertEquals(46, scores[2].cumulativeScore)
    }

    @Test
    fun tenthFrame_strikeFinish() {
        // 9 frames gutter, 10th X X X = 30 in 10th
        val frames = buildFrames(
            *Array(9) { Frame(firstRoll = 0, secondRoll = 0) },
            Frame(firstRoll = 10, secondRoll = 10, bonusRoll = 10),
        )
        assertEquals(30, BowlingScoreCalculator.calculateTotalScore(frames))
    }

    @Test
    fun tenthFrame_spareFinish() {
        // 9 frames gutter, 10th 7/ 3 = 17
        val frames = buildFrames(
            *Array(9) { Frame(firstRoll = 0, secondRoll = 0) },
            Frame(firstRoll = 7, secondRoll = 3, bonusRoll = 3),
        )
        assertEquals(17, BowlingScoreCalculator.calculateTotalScore(frames))
    }

    @Test
    fun incompleteGame_totalIsNull() {
        val frames = buildFrames(
            Frame(firstRoll = 10),
            Frame(firstRoll = 7),
        )
        assertNull(BowlingScoreCalculator.calculateTotalScore(frames))
        val scores = BowlingScoreCalculator.calculateFrameScores(frames)
        assertNull(scores[0].cumulativeScore)
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
