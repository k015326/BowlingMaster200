package com.example.bowlingmaster200.ocr.adapter

import com.example.bowlingmaster200.domain.calculator.BowlingScoreCalculator
import com.example.bowlingmaster200.domain.model.GameScore
import com.example.bowlingmaster200.domain.model.SavedGame
import com.example.bowlingmaster200.domain.validator.FrameValidator
import com.example.bowlingmaster200.domain.validator.ValidationResult
import com.example.bowlingmaster200.ocr.analyzer.ScoreSheetAnalysisResult

/**
 * [ScoreSheetAnalysisResult] から [SavedGame] を取り出し Calculator でスコア計算する。
 */
class OcrToCalculatorAdapter {

    fun adapt(analysis: ScoreSheetAnalysisResult): OcrProcessUiResult {
        val savedGame = analysis.savedGame
            ?: return OcrProcessUiResult(
                warnings = analysis.warnings,
                confidence = analysis.confidence,
                engineId = analysis.engineId,
                errorMessage = "OCR result has no game data",
            )

        val validation = FrameValidator.validateGame(savedGame.frames)
        if (validation is ValidationResult.Invalid) {
            return OcrProcessUiResult(
                savedGame = savedGame,
                warnings = analysis.warnings,
                confidence = analysis.confidence,
                engineId = analysis.engineId,
                errorMessage = validation.reason,
            )
        }

        val gameScore = BowlingScoreCalculator.calculateGameScore(savedGame.frames)
        if (!gameScore.isComplete) {
            return OcrProcessUiResult(
                savedGame = savedGame,
                gameScore = gameScore,
                warnings = analysis.warnings,
                confidence = analysis.confidence,
                engineId = analysis.engineId,
                errorMessage = "Incomplete game score from OCR data",
            )
        }

        return OcrProcessUiResult(
            savedGame = savedGame,
            gameScore = gameScore,
            warnings = analysis.warnings,
            confidence = analysis.confidence,
            engineId = analysis.engineId,
        )
    }
}

/**
 * OCR → Calculator 処理結果（UI 表示用）。
 */
data class OcrProcessUiResult(
    val savedGame: SavedGame? = null,
    val gameScore: GameScore? = null,
    val warnings: List<String> = emptyList(),
    val confidence: Float? = null,
    val engineId: String? = null,
    val errorMessage: String? = null,
) {
    val isSuccess: Boolean
        get() = errorMessage == null && gameScore?.isComplete == true
}
