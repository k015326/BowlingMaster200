package com.example.bowlingmaster200.ocr.analyzer

import com.example.bowlingmaster200.domain.model.Frame
import com.example.bowlingmaster200.domain.model.GameMetadata
import com.example.bowlingmaster200.domain.model.SavedGame
import com.example.bowlingmaster200.ocr.pipeline.OcrResult
import com.example.bowlingmaster200.ocr.service.FakeOcrService

/**
 * OCR 文字列から [SavedGame] を生成する責務。
 * 将来 SpareMaster の ScoreSheetLayout / SymbolRowAnalyzer ロジックを移植。
 */
class BowlingScoreSheetAnalyzer {

    fun analyze(ocrResult: OcrResult): ScoreSheetAnalysisResult {
        val warnings = mutableListOf<String>()
        val frameMap = parseFrameMap(ocrResult.rawText, warnings)
        val isComplete = frameMap.size == Frame.FRAME_COUNT &&
            frameMap.values.all { it.firstRoll != null }

        if (!isComplete) {
            warnings.add("Incomplete frame data from OCR")
        }

        val now = System.currentTimeMillis()
        val savedGame = SavedGame(
            metadata = GameMetadata(
                playedAt = now,
                createdAt = now,
                location = "OCR Import",
            ),
            frames = normalizeFrames(frameMap),
        )

        return ScoreSheetAnalysisResult(
            savedGame = savedGame,
            confidence = ocrResult.confidence,
            warnings = warnings,
            engineId = ocrResult.engineId,
        )
    }

    private fun parseFrameMap(rawText: String, warnings: MutableList<String>): Map<Int, Frame> {
        val frameMap = mutableMapOf<Int, Frame>()

        rawText.lines()
            .map { it.trim() }
            .filter { it.startsWith("F", ignoreCase = true) && it.contains(":") }
            .forEach { line ->
                val colonIndex = line.indexOf(':')
                val framePart = line.substring(0, colonIndex).removePrefix("F").removePrefix("f")
                val frameIndex = framePart.trim().toIntOrNull()
                if (frameIndex == null || frameIndex !in 1..Frame.FRAME_COUNT) {
                    warnings.add("Skipped invalid frame line: $line")
                    return@forEach
                }

                val rolls = line.substring(colonIndex + 1).split(",")
                    .map { it.trim().toIntOrNull() }

                frameMap[frameIndex] = Frame(
                    firstRoll = rolls.getOrNull(0),
                    secondRoll = rolls.getOrNull(1),
                    bonusRoll = if (frameIndex == Frame.FRAME_COUNT) rolls.getOrNull(2) else null,
                )
            }

        if (frameMap.isEmpty() && rawText.isNotBlank()) {
            warnings.add("No frame lines parsed; using empty frames")
        }

        return frameMap
    }

    private fun normalizeFrames(frameMap: Map<Int, Frame>): List<Frame> {
        return (1..Frame.FRAME_COUNT).map { index ->
            frameMap[index] ?: Frame()
        }
    }

    companion object {
        /** FakeOcrService 用の即席解析。 */
        fun analyzeFake(): ScoreSheetAnalysisResult {
            val analyzer = BowlingScoreSheetAnalyzer()
            val fakeResult = OcrResult(
                rawText = FakeOcrService.FAKE_SCORE_SHEET_TEXT,
                engineId = FakeOcrService.ENGINE_ID,
                confidence = 1.0f,
            )
            return analyzer.analyze(fakeResult)
        }
    }
}

data class ScoreSheetAnalysisResult(
    val savedGame: SavedGame?,
    val confidence: Float?,
    val warnings: List<String> = emptyList(),
    val engineId: String? = null,
)
