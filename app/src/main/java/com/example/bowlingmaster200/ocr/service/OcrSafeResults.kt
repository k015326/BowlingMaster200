package com.example.bowlingmaster200.ocr.service

import com.example.bowlingmaster200.domain.model.Frame
import com.example.bowlingmaster200.domain.model.GameMetadata
import com.example.bowlingmaster200.domain.model.SavedGame
import com.example.bowlingmaster200.ocr.analyzer.ScoreSheetAnalysisResult
import com.example.bowlingmaster200.ocr.pipeline.OcrInput
import com.example.bowlingmaster200.ocr.pipeline.OcrPipelineResult
import com.example.bowlingmaster200.ocr.pipeline.OcrResult

/**
 * OCR パイプライン各段階の安全なフォールバック結果。
 */
internal object OcrSafeResults {

    fun emptyOcrResult(
        engineId: String = "error",
        reason: String? = null,
    ): OcrResult {
        return OcrResult(
            rawText = "",
            lines = emptyList(),
            confidence = 0f,
            engineId = engineId,
            debugInfo = buildMap {
                put("safe", "true")
                reason?.let { put("error", it) }
            },
        )
    }

    fun safeAnalysis(
        ocrResult: OcrResult,
        warnings: List<String>,
    ): ScoreSheetAnalysisResult {
        val now = System.currentTimeMillis()
        val emptyFrames = List(Frame.FRAME_COUNT) { Frame() }
        return ScoreSheetAnalysisResult(
            savedGame = SavedGame(
                metadata = GameMetadata(
                    playedAt = now,
                    createdAt = now,
                    location = "OCR Import",
                ),
                frames = emptyFrames,
            ),
            confidence = ocrResult.confidence,
            warnings = warnings,
            engineId = ocrResult.engineId,
        )
    }

    fun safePipelineResult(
        input: OcrInput,
        reason: String,
        engineId: String = "error",
    ): OcrPipelineResult {
        val ocrResult = emptyOcrResult(engineId = engineId, reason = reason)
        return OcrPipelineResult(
            input = input,
            ocrResult = ocrResult,
            analysis = safeAnalysis(ocrResult, warnings = listOf(reason)),
        )
    }
}
