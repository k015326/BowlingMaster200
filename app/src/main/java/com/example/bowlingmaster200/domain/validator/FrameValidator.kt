package com.example.bowlingmaster200.domain.validator

import com.example.bowlingmaster200.domain.model.Frame
import com.example.bowlingmaster200.domain.model.Roll

object FrameValidator {

    fun validateGame(frames: List<Frame>): ValidationResult {
        if (frames.size != Frame.FRAME_COUNT) {
            return ValidationResult.Invalid(
                "Game must have ${Frame.FRAME_COUNT} frames, got ${frames.size}",
            )
        }
        frames.forEachIndexed { index, frame ->
            validateFrame(frame, index)?.let { return it }
        }
        return ValidationResult.Valid
    }

    fun validateFrame(frame: Frame, frameIndex: Int): ValidationResult? {
        require(frameIndex in 0 until Frame.FRAME_COUNT) {
            "frameIndex must be 0..${Frame.LAST_FRAME_INDEX}, got $frameIndex"
        }
        return if (frameIndex == Frame.LAST_FRAME_INDEX) {
            validateTenthFrame(frame)
        } else {
            validateRegularFrame(frame, frameIndex + 1)
        }
    }

    private fun validateRegularFrame(frame: Frame, frameNumber: Int): ValidationResult? {
        frame.firstRoll?.let { first ->
            pinRange(first)?.let { return it }

            if (frame.secondRoll != null && first == Roll.MAX_PINS) {
                return ValidationResult.Invalid(
                    "Frame $frameNumber: secondRoll is not allowed after a strike",
                )
            }
        }

        frame.secondRoll?.let { second ->
            pinRange(second)?.let { return it }
            val first = frame.firstRoll
                ?: return ValidationResult.Invalid(
                    "Frame $frameNumber: secondRoll without firstRoll",
                )
            if (first + second > Roll.FRAME_PINS) {
                return ValidationResult.Invalid(
                    "Frame $frameNumber: firstRoll($first) + secondRoll($second) exceeds ${Roll.FRAME_PINS}",
                )
            }
        }

        if (frame.bonusRoll != null) {
            return ValidationResult.Invalid(
                "Frame $frameNumber: bonusRoll is only allowed in the 10th frame",
            )
        }

        return null
    }

    private fun validateTenthFrame(frame: Frame): ValidationResult? {
        val first = frame.firstRoll ?: run {
            if (frame.secondRoll != null || frame.bonusRoll != null) {
                return ValidationResult.Invalid("10th frame: rolls exist without firstRoll")
            }
            return null
        }
        pinRange(first)?.let { return it }

        val second = frame.secondRoll ?: run {
            if (frame.bonusRoll != null) {
                return ValidationResult.Invalid("10th frame: bonusRoll without secondRoll")
            }
            return null
        }
        pinRange(second)?.let { return it }

        if (first != Roll.MAX_PINS && first + second > Roll.FRAME_PINS) {
            return ValidationResult.Invalid(
                "10th frame: firstRoll($first) + secondRoll($second) exceeds ${Roll.FRAME_PINS}",
            )
        }

        if (first == Roll.MAX_PINS && second < Roll.MAX_PINS && second + (frame.bonusRoll ?: 0) > Roll.FRAME_PINS) {
            if (frame.bonusRoll != null) {
                return ValidationResult.Invalid(
                    "10th frame: secondRoll($second) + bonusRoll(${frame.bonusRoll}) exceeds ${Roll.FRAME_PINS}",
                )
            }
        }

        frame.bonusRoll?.let { bonus ->
            pinRange(bonus)?.let { return it }

            val needsBonus = first == Roll.MAX_PINS || first + second == Roll.FRAME_PINS
            if (!needsBonus) {
                return ValidationResult.Invalid(
                    "10th frame: bonusRoll is not allowed on an open frame",
                )
            }

            if (first != Roll.MAX_PINS && first + second == Roll.FRAME_PINS) {
                // Spare on first two rolls — bonus has no pair limit
                return null
            }

            // Strike start: second roll exists, validate bonus against second if not strike
            if (first == Roll.MAX_PINS && second < Roll.MAX_PINS && second + bonus > Roll.FRAME_PINS) {
                return ValidationResult.Invalid(
                    "10th frame: secondRoll($second) + bonusRoll($bonus) exceeds ${Roll.FRAME_PINS}",
                )
            }
        }

        return null
    }

    private fun pinRange(pins: Int): ValidationResult.Invalid? {
        if (pins !in Roll.MIN_PINS..Roll.MAX_PINS) {
            return ValidationResult.Invalid(
                "Pin count must be ${Roll.MIN_PINS}..${Roll.MAX_PINS}, got $pins",
            )
        }
        return null
    }
}
