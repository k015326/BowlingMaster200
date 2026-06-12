package com.example.bowlingmaster200.ui.games

import com.example.bowlingmaster200.domain.model.FrameType
import com.example.bowlingmaster200.domain.model.Roll

data class FrameInputUiState(
    val frameIndex: Int,
    val firstRollText: String = "",
    val secondRollText: String = "",
    val bonusRollText: String = "",
    val firstRollKey: String? = null,
    val secondRollKey: String? = null,
    val bonusRollKey: String? = null,
    val framePins: Int? = null,
    val cumulativeScore: Int? = null,
    val frameType: FrameType = FrameType.INCOMPLETE,
) {
    val isTenthFrame: Boolean get() = frameIndex == 10

    val firstRollValue: Int? get() = firstRollText.toIntOrNull()
    val isStrikeFirstRoll: Boolean get() = firstRollValue == Roll.MAX_PINS

    fun displayTextForRoll(rollIndex: Int): String {
        val raw = when (rollIndex) {
            0 -> firstRollText
            1 -> secondRollText
            2 -> bonusRollText
            else -> ""
        }
        if (raw.isBlank()) return ""
        return when (rollIndex) {
            1 -> if (isSpareSecondRoll()) "/" else formatRollDisplay(raw, secondRollKey)
            2 -> if (isSpareBonusRoll()) "/" else formatRollDisplay(raw, bonusRollKey)
            else -> formatRollDisplay(raw, firstRollKey)
        }
    }

    private fun formatRollDisplay(raw: String, inputKey: String?): String {
        if (raw == "0" && inputKey == "-") return "-"
        return raw
    }

    private fun isSpareSecondRoll(): Boolean {
        val first = firstRollText.toIntOrNull() ?: return false
        val second = secondRollText.toIntOrNull() ?: return false
        if (first == Roll.MAX_PINS) return false
        return first + second == Roll.FRAME_PINS
    }

    private fun isSpareBonusRoll(): Boolean {
        if (!isTenthFrame) return false
        val first = firstRollText.toIntOrNull() ?: return false
        val second = secondRollText.toIntOrNull() ?: return false
        val third = bonusRollText.toIntOrNull() ?: return false
        return first == Roll.MAX_PINS &&
            second in 1 until Roll.MAX_PINS &&
            second + third == Roll.FRAME_PINS
    }
}

data class GamesUiState(
    val frames: List<FrameInputUiState> = emptyList(),
    val totalScore: Int? = null,
    val isComplete: Boolean = false,
    val validationError: String? = null,
    val saveMessage: String? = null,
    val isSaving: Boolean = false,
    val selectedFrameIndex: Int = 0,
    val selectedRollIndex: Int = 0,
) {
    companion object {
        fun initial(): GamesUiState {
            val frames = (1..10).map { index ->
                FrameInputUiState(frameIndex = index)
            }
            return GamesUiState(frames = frames)
        }
    }
}
