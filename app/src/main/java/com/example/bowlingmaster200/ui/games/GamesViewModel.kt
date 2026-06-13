package com.example.bowlingmaster200.ui.games

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bowlingmaster200.data.repository.BowlingRepository
import com.example.bowlingmaster200.domain.calculator.BowlingScoreCalculator
import com.example.bowlingmaster200.domain.model.Frame
import com.example.bowlingmaster200.domain.model.GameMetadata
import com.example.bowlingmaster200.domain.model.Roll
import com.example.bowlingmaster200.domain.model.SavedGame
import com.example.bowlingmaster200.domain.validator.FrameValidator
import com.example.bowlingmaster200.domain.validator.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GamesViewModel(
    application: Application,
    private val repository: BowlingRepository = BowlingRepository.create(application),
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(GamesUiState.initial())
    val uiState: StateFlow<GamesUiState> = _uiState.asStateFlow()

    fun saveGame() {
        val current = _uiState.value
        val domainFrames = toDomainFrames(current.frames)
        val validation = FrameValidator.validateGame(domainFrames)
        if (validation is ValidationResult.Invalid) {
            _uiState.update { it.copy(validationError = validation.reason, saveMessage = null) }
            return
        }

        val gameScore = BowlingScoreCalculator.calculateGameScore(domainFrames)
        if (!gameScore.isComplete) {
            _uiState.update {
                it.copy(
                    validationError = "全10フレームの入力を完了してください",
                    saveMessage = null,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, validationError = null, saveMessage = null) }
            try {
                val now = System.currentTimeMillis()
                val savedGame = SavedGame(
                    metadata = GameMetadata(
                        playedAt = now,
                        totalScore = gameScore.totalScore,
                        createdAt = now,
                    ),
                    frames = domainFrames,
                )
                repository.saveGame(savedGame)
                resetGame()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        validationError = e.message ?: "保存に失敗しました",
                    )
                }
            }
        }
    }

    fun clearSaveMessage() {
        _uiState.update { it.copy(saveMessage = null) }
    }

    fun resetGame() {
        _uiState.value = GamesUiState.initial()
    }

    fun selectCell(frameIndex: Int, rollIndex: Int) {
        val arrayIndex = frameIndex - 1
        if (arrayIndex !in 0 until Frame.FRAME_COUNT) return
        if (rollIndex !in 0..2) return
        _uiState.update { state ->
            if (!isCellEditable(state.frames, arrayIndex, rollIndex)) return@update state
            state.copy(
                selectedFrameIndex = arrayIndex,
                selectedRollIndex = rollIndex,
            )
        }
    }

    fun onKeyInput(key: String) {
        if (key == "DEL") {
            deleteInput()
            return
        }

        val resolvedText = resolveKeypadKey(key) ?: return

        _uiState.update { state ->
            val frameIndex = state.selectedFrameIndex
            val rollIndex = state.selectedRollIndex
            if (!isCellEditable(state.frames, frameIndex, rollIndex)) return@update state
            if (!isValidKeypadKey(state.frames, frameIndex, rollIndex, key, resolvedText)) {
                return@update state
            }

            val frames = state.frames.toMutableList()
            val textToSet = when (key) {
                "/" -> computeSparePins(frames, frameIndex, rollIndex).toString()
                in "1".."9" -> sanitizeRollInput(key)
                else -> resolvedText
            }
            val inputKey = if (key in setOf("G", "F", "-")) key else null
            setRollText(frames, frameIndex, rollIndex, textToSet, inputKey)
            val (nextFrameIndex, nextRollIndex) = moveCursorNext(frames, frameIndex, rollIndex)
            recompute(
                state.copy(
                    frames = frames,
                    selectedFrameIndex = nextFrameIndex,
                    selectedRollIndex = nextRollIndex,
                    saveMessage = null,
                ),
            )
        }
    }

    fun deleteInput() {
        _uiState.update { state ->
            val frameIndex = state.selectedFrameIndex
            val rollIndex = state.selectedRollIndex
            val frames = state.frames.toMutableList()
            val currentText = getRollText(frames, frameIndex, rollIndex)

            if (currentText.isNotEmpty()) {
                setRollText(frames, frameIndex, rollIndex, "")
                return@update recompute(state.copy(frames = frames, saveMessage = null))
            }

            val previous = moveCursorPrevious(state.frames, frameIndex, rollIndex)
                ?: return@update state
            val (prevFrameIndex, prevRollIndex) = previous
            setRollText(frames, prevFrameIndex, prevRollIndex, "")
            recompute(
                state.copy(
                    frames = frames,
                    selectedFrameIndex = prevFrameIndex,
                    selectedRollIndex = prevRollIndex,
                    saveMessage = null,
                ),
            )
        }
    }

    private fun getRollText(
        frames: List<FrameInputUiState>,
        frameIndex: Int,
        rollIndex: Int,
    ): String {
        val frame = frames[frameIndex]
        return when (rollIndex) {
            0 -> frame.firstRollText
            1 -> frame.secondRollText
            2 -> frame.bonusRollText
            else -> ""
        }
    }

    private fun setRollText(
        frames: MutableList<FrameInputUiState>,
        frameIndex: Int,
        rollIndex: Int,
        text: String,
        inputKey: String? = null,
    ) {
        val frame = frames[frameIndex]
        val rollKey = if (text.isEmpty()) null else inputKey
        val updated = when (rollIndex) {
            0 -> {
                var result = frame.copy(firstRollText = text, firstRollKey = rollKey)
                if (frameIndex < Frame.LAST_FRAME_INDEX && result.isStrikeFirstRoll) {
                    result = result.copy(secondRollText = "", secondRollKey = null)
                }
                result
            }
            1 -> frame.copy(secondRollText = text, secondRollKey = rollKey)
            2 -> frame.copy(bonusRollText = text, bonusRollKey = rollKey)
            else -> frame
        }
        frames[frameIndex] = updated
    }

    private fun isCellEditable(
        frames: List<FrameInputUiState>,
        frameIndex: Int,
        rollIndex: Int,
    ): Boolean {
        if (frameIndex !in 0 until Frame.FRAME_COUNT) return false
        if (rollIndex !in 0..2) return false

        val frame = frames[frameIndex]
        return when (rollIndex) {
            0 -> true
            1 -> {
                if (frameIndex < Frame.LAST_FRAME_INDEX) {
                    !frame.isStrikeFirstRoll
                } else {
                    true
                }
            }
            2 -> {
                if (frameIndex != Frame.LAST_FRAME_INDEX) return false
                val first = frame.firstRollText.toIntOrNull() ?: return false
                val second = frame.secondRollText.toIntOrNull() ?: return false
                first == Roll.MAX_PINS || first + second == Roll.FRAME_PINS
            }
            else -> false
        }
    }

    private fun isValidKeypadDigit(
        frames: List<FrameInputUiState>,
        frameIndex: Int,
        rollIndex: Int,
        digit: Int,
    ): Boolean {
        val frame = frames[frameIndex]
        return when (rollIndex) {
            0 -> digit in 1..9
            1 -> {
                val first = frame.firstRollText.toIntOrNull() ?: return false
                if (frameIndex < Frame.LAST_FRAME_INDEX && first == Roll.MAX_PINS) return false
                first + digit <= Roll.FRAME_PINS
            }
            2 -> {
                if (frameIndex != Frame.LAST_FRAME_INDEX) return false
                val first = frame.firstRollText.toIntOrNull() ?: return false
                val second = frame.secondRollText.toIntOrNull() ?: return false
                if (first != Roll.MAX_PINS && first + second != Roll.FRAME_PINS) return false
                if (first == Roll.MAX_PINS && second < Roll.MAX_PINS) {
                    return second + digit <= Roll.FRAME_PINS
                }
                true
            }
            else -> false
        }
    }

    private fun resolveKeypadKey(key: String): String? {
        return when (key) {
            "X" -> "10"
            "G", "F", "-" -> "0"
            "/" -> "/"
            else -> if (key.length == 1 && key[0] in '1'..'9') key else null
        }
    }

    private fun isValidKeypadKey(
        frames: List<FrameInputUiState>,
        frameIndex: Int,
        rollIndex: Int,
        key: String,
        resolvedText: String,
    ): Boolean {
        return when (key) {
            "X" -> rollIndex == 0
            "G", "F", "-" -> isValidKeypadZero(frames, frameIndex, rollIndex)
            "/" -> isValidKeypadSpare(frames, frameIndex, rollIndex)
            else -> isValidKeypadDigit(frames, frameIndex, rollIndex, resolvedText.toInt())
        }
    }

    private fun isValidKeypadSpare(
        frames: List<FrameInputUiState>,
        frameIndex: Int,
        rollIndex: Int,
    ): Boolean {
        val frame = frames[frameIndex]
        return when (rollIndex) {
            0 -> false
            1 -> {
                val first = frame.firstRollText.toIntOrNull() ?: return false
                first != Roll.MAX_PINS
            }
            2 -> {
                if (frameIndex != Frame.LAST_FRAME_INDEX) return false
                val first = frame.firstRollText.toIntOrNull() ?: return false
                val second = frame.secondRollText.toIntOrNull() ?: return false
                first == Roll.MAX_PINS && second in 1 until Roll.MAX_PINS
            }
            else -> false
        }
    }

    private fun computeSparePins(
        frames: List<FrameInputUiState>,
        frameIndex: Int,
        rollIndex: Int,
    ): Int {
        val frame = frames[frameIndex]
        return when (rollIndex) {
            1 -> Roll.FRAME_PINS - frame.firstRollText.toIntOrNull()!!
            2 -> Roll.FRAME_PINS - frame.secondRollText.toIntOrNull()!!
            else -> error("computeSparePins called for invalid rollIndex: $rollIndex")
        }
    }

    private fun isValidKeypadZero(
        frames: List<FrameInputUiState>,
        frameIndex: Int,
        rollIndex: Int,
    ): Boolean {
        val frame = frames[frameIndex]
        return when (rollIndex) {
            0 -> true
            1 -> {
                val first = frame.firstRollText.toIntOrNull() ?: return false
                if (frameIndex < Frame.LAST_FRAME_INDEX && first == Roll.MAX_PINS) return false
                first + Roll.MIN_PINS <= Roll.FRAME_PINS
            }
            2 -> {
                if (frameIndex != Frame.LAST_FRAME_INDEX) return false
                val first = frame.firstRollText.toIntOrNull() ?: return false
                val second = frame.secondRollText.toIntOrNull() ?: return false
                first == Roll.MAX_PINS || first + second == Roll.FRAME_PINS
            }
            else -> false
        }
    }

    private fun moveCursorNext(
        frames: List<FrameInputUiState>,
        frameIndex: Int,
        rollIndex: Int,
    ): Pair<Int, Int> {
        return when (rollIndex) {
            0 -> {
                if (isCellEditable(frames, frameIndex, 1)) {
                    frameIndex to 1
                } else if (frameIndex < Frame.LAST_FRAME_INDEX) {
                    (frameIndex + 1) to 0
                } else {
                    frameIndex to 0
                }
            }
            1 -> {
                if (frameIndex < Frame.LAST_FRAME_INDEX) {
                    (frameIndex + 1) to 0
                } else {
                    val frame = frames[frameIndex]
                    val first = frame.firstRollText.toIntOrNull()
                    val second = frame.secondRollText.toIntOrNull()
                    if (
                        first != null &&
                        second != null &&
                        (first == Roll.MAX_PINS || first + second == Roll.FRAME_PINS) &&
                        isCellEditable(frames, frameIndex, 2)
                    ) {
                        frameIndex to 2
                    } else {
                        frameIndex to 1
                    }
                }
            }
            else -> frameIndex to rollIndex
        }
    }

    private fun moveCursorPrevious(
        frames: List<FrameInputUiState>,
        frameIndex: Int,
        rollIndex: Int,
    ): Pair<Int, Int>? {
        var f = frameIndex
        var r = rollIndex - 1
        if (r < 0) {
            f--
            r = if (f >= 0) maxRollIndexForFrame(f) else -1
        }
        while (f >= 0) {
            while (r >= 0) {
                if (isCellEditable(frames, f, r)) return f to r
                r--
            }
            f--
            r = if (f >= 0) maxRollIndexForFrame(f) else -1
        }
        return null
    }

    private fun maxRollIndexForFrame(frameIndex: Int): Int {
        return if (frameIndex == Frame.LAST_FRAME_INDEX) 2 else 1
    }

    private fun recompute(state: GamesUiState): GamesUiState {
        val domainFrames = toDomainFrames(state.frames)
        val validationError = (FrameValidator.validateGame(domainFrames) as? ValidationResult.Invalid)?.reason
        val gameScore = BowlingScoreCalculator.calculateGameScore(domainFrames)
        val updatedFrames = state.frames.mapIndexed { index, ui ->
            val score = gameScore.frameScores[index]
            ui.copy(
                framePins = score.framePins,
                cumulativeScore = score.cumulativeScore,
                frameType = score.frameType,
            )
        }
        return state.copy(
            frames = updatedFrames,
            totalScore = gameScore.totalScore,
            isComplete = gameScore.isComplete,
            validationError = validationError,
        )
    }

    private fun toDomainFrames(frames: List<FrameInputUiState>): List<Frame> {
        return frames.map { ui ->
            Frame(
                firstRoll = ui.firstRollText.toIntOrNull(),
                secondRoll = ui.secondRollText.takeIf { it.isNotBlank() }?.toIntOrNull(),
                bonusRoll = if (ui.isTenthFrame) {
                    ui.bonusRollText.takeIf { it.isNotBlank() }?.toIntOrNull()
                } else {
                    null
                },
            )
        }
    }

    private fun sanitizeRollInput(text: String): String {
        return text.filter { it.isDigit() }.take(2)
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GamesViewModel(application) as T
                }
            }
        }
    }
}
