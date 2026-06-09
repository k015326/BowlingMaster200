package com.example.bowlingmaster200.ui.games

import com.example.bowlingmaster200.domain.model.FrameType

data class FrameInputUiState(
    val frameIndex: Int,
    val firstRollText: String = "",
    val secondRollText: String = "",
    val bonusRollText: String = "",
    val framePins: Int? = null,
    val cumulativeScore: Int? = null,
    val frameType: FrameType = FrameType.INCOMPLETE,
) {
    val isTenthFrame: Boolean get() = frameIndex == 10

    val firstRollValue: Int? get() = firstRollText.toIntOrNull()
    val isStrikeFirstRoll: Boolean get() = firstRollValue == 10
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
