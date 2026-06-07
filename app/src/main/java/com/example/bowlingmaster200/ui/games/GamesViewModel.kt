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

    fun updateFirstRoll(frameIndex: Int, text: String) {
        val sanitized = sanitizeRollInput(text)
        _uiState.update { state ->
            val index = frameIndex - 1
            val frames = state.frames.toMutableList()
            val current = frames[index]
            var updated = current.copy(firstRollText = sanitized)
            if (!updated.isTenthFrame && updated.isStrikeFirstRoll) {
                updated = updated.copy(secondRollText = "")
            }
            frames[index] = updated
            recompute(state.copy(frames = frames, saveMessage = null))
        }
    }

    fun updateSecondRoll(frameIndex: Int, text: String) {
        val sanitized = sanitizeRollInput(text)
        _uiState.update { state ->
            val index = frameIndex - 1
            val frames = state.frames.toMutableList()
            frames[index] = frames[index].copy(secondRollText = sanitized)
            recompute(state.copy(frames = frames, saveMessage = null))
        }
    }

    fun updateBonusRoll(text: String) {
        val sanitized = sanitizeRollInput(text)
        _uiState.update { state ->
            val frames = state.frames.toMutableList()
            val index = Frame.LAST_FRAME_INDEX
            frames[index] = frames[index].copy(bonusRollText = sanitized)
            recompute(state.copy(frames = frames, saveMessage = null))
        }
    }

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
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveMessage = "保存しました（Total: ${gameScore.totalScore}）",
                        validationError = null,
                    )
                }
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
