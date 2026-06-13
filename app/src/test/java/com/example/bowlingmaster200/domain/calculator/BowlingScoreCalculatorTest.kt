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
    fun strikeFrame_scoresWithNextTwoRolls() {
        val frames = buildFrames(
            Frame(firstRoll = Roll.MAX_PINS),
            Frame(firstRoll = 8, secondRoll = 1),
            Frame(firstRoll = 7, secondRoll = 2),
        )
        val scores = BowlingScoreCalculator.calculateFrameScores(frames)

        assertEquals(FrameType.STRIKE, scores[0].frameType)
        assertEquals(19, scores[0].cumulativeScore)
        assertEquals(28, scores[1].cumulativeScore)
        assertEquals(37, scores[2].cumulativeScore)
    }

    @Test
    fun strikeFrame_withoutEnoughBonusRolls_stopsScoring() {
        val frames = buildFrames(
            Frame(firstRoll = 9, secondRoll = 0),
            Frame(firstRoll = Roll.MAX_PINS),
            Frame(),
        )
        val scores = BowlingScoreCalculator.calculateFrameScores(frames)

        assertEquals(9, scores[0].cumulativeScore)
        assertEquals(FrameType.STRIKE, scores[1].frameType)
        assertNull(scores[1].cumulativeScore)
        assertNull(scores[2].cumulativeScore)
        assertNull(BowlingScoreCalculator.calculateTotalScore(frames))
    }

    @Test
    fun consecutiveStrikes_scoresWithCrossFrameBonus() {
        val frames = buildFrames(
            Frame(firstRoll = Roll.MAX_PINS),
            Frame(firstRoll = Roll.MAX_PINS),
            Frame(firstRoll = 8, secondRoll = 1),
        )
        val scores = BowlingScoreCalculator.calculateFrameScores(frames)

        assertEquals(28, scores[0].cumulativeScore)
        assertEquals(47, scores[1].cumulativeScore)
        assertEquals(56, scores[2].cumulativeScore)
    }

    @Test
    fun tenthFrameStrike_withoutEnoughRolls_stopsScoring() {
        val frames = List(Frame.LAST_FRAME_INDEX) { Frame(firstRoll = 9, secondRoll = 0) } +
            Frame(firstRoll = Roll.MAX_PINS, secondRoll = 8)
        val scores = BowlingScoreCalculator.calculateFrameScores(frames)

        assertEquals(FrameType.STRIKE, scores[9].frameType)
        assertNull(scores[9].cumulativeScore)
        assertNull(BowlingScoreCalculator.calculateTotalScore(frames))
    }

    @Test
    fun spareFrame_scoresWithNextRollBonus() {
        val frames = buildFrames(
            Frame(firstRoll = 9, secondRoll = 0),
            Frame(firstRoll = 7, secondRoll = 3),
            Frame(firstRoll = 8, secondRoll = 1),
        )
        val scores = BowlingScoreCalculator.calculateFrameScores(frames)

        assertEquals(9, scores[0].cumulativeScore)
        assertEquals(FrameType.SPARE, scores[1].frameType)
        assertEquals(10, scores[1].framePins)
        assertEquals(27, scores[1].cumulativeScore)
        assertEquals(36, scores[2].cumulativeScore)
    }

    @Test
    fun spareFrame_withoutNextRoll_stopsScoring() {
        val frames = buildFrames(
            Frame(firstRoll = 9, secondRoll = 0),
            Frame(firstRoll = 7, secondRoll = 3),
            Frame(),
        )
        val scores = BowlingScoreCalculator.calculateFrameScores(frames)

        assertEquals(9, scores[0].cumulativeScore)
        assertEquals(FrameType.SPARE, scores[1].frameType)
        assertNull(scores[1].cumulativeScore)
        assertNull(scores[2].cumulativeScore)
    }

    @Test
    fun spareFrame_bonusFromNextFrameStrike() {
        val frames = buildFrames(
            Frame(firstRoll = 7, secondRoll = 3),
            Frame(firstRoll = Roll.MAX_PINS),
            Frame(firstRoll = 8, secondRoll = 1),
        )
        val scores = BowlingScoreCalculator.calculateFrameScores(frames)

        assertEquals(20, scores[0].cumulativeScore)
        assertEquals(FrameType.STRIKE, scores[1].frameType)
        assertEquals(39, scores[1].cumulativeScore)
        assertEquals(48, scores[2].cumulativeScore)
    }

    @Test
    fun tenthFrameSpare_withoutBonusRoll_stopsScoring() {
        val frames = List(Frame.LAST_FRAME_INDEX) { Frame(firstRoll = 9, secondRoll = 0) } +
            Frame(firstRoll = 7, secondRoll = 3)
        val scores = BowlingScoreCalculator.calculateFrameScores(frames)

        assertEquals(FrameType.SPARE, scores[9].frameType)
        assertNull(scores[9].cumulativeScore)
        assertNull(BowlingScoreCalculator.calculateTotalScore(frames))
    }

    @Test
    fun ninthFrameSpare_scoresWithTenthFrameFirstRoll() {
        val frames = List(Frame.LAST_FRAME_INDEX - 1) { Frame(firstRoll = 9, secondRoll = 0) } +
            Frame(firstRoll = 7, secondRoll = 3) +
            Frame(firstRoll = 5, secondRoll = 2)
        val scores = BowlingScoreCalculator.calculateFrameScores(frames)

        assertEquals(FrameType.SPARE, scores[8].frameType)
        assertEquals(87, scores[8].cumulativeScore)
        assertEquals(94, scores[9].cumulativeScore)
    }

    @Test
    fun ninthFrameStrike_scoresWithTenthFrameRolls() {
        val frames = List(Frame.LAST_FRAME_INDEX - 1) { Frame(firstRoll = 9, secondRoll = 0) } +
            Frame(firstRoll = Roll.MAX_PINS) +
            Frame(firstRoll = 8, secondRoll = 1)
        val scores = BowlingScoreCalculator.calculateFrameScores(frames)

        assertEquals(FrameType.STRIKE, scores[8].frameType)
        assertEquals(91, scores[8].cumulativeScore)
        assertEquals(100, scores[9].cumulativeScore)
    }

    @Test
    fun tenthFrameTripleStrike_scoresThirty() {
        val frames = gutterFramesThrough9() +
            Frame(firstRoll = Roll.MAX_PINS, secondRoll = Roll.MAX_PINS, bonusRoll = Roll.MAX_PINS)
        assertEquals(30, BowlingScoreCalculator.calculateTotalScore(frames))
    }

    @Test
    fun tenthFrameStrikeSpare_scoresTwenty() {
        val frames = gutterFramesThrough9() +
            Frame(firstRoll = Roll.MAX_PINS, secondRoll = 7, bonusRoll = 3)
        assertEquals(20, BowlingScoreCalculator.calculateTotalScore(frames))
    }

    @Test
    fun tenthFrameStrikeOpen_scoresNineteen() {
        val frames = gutterFramesThrough9() +
            Frame(firstRoll = Roll.MAX_PINS, secondRoll = 7, bonusRoll = 2)
        assertEquals(19, BowlingScoreCalculator.calculateTotalScore(frames))
    }

    @Test
    fun tenthFrameSpare_scoresEighteen() {
        val frames = gutterFramesThrough9() +
            Frame(firstRoll = 7, secondRoll = 3, bonusRoll = 8)
        assertEquals(18, BowlingScoreCalculator.calculateTotalScore(frames))
    }

    @Test
    fun tenthFrameOpen_scoresNine() {
        val frames = gutterFramesThrough9() +
            Frame(firstRoll = 7, secondRoll = 2)
        assertEquals(9, BowlingScoreCalculator.calculateTotalScore(frames))
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

    private fun gutterFramesThrough9(): List<Frame> {
        return List(Frame.LAST_FRAME_INDEX) { Frame(firstRoll = 0, secondRoll = 0) }
    }
}
