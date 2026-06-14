package com.example.bowlingmaster200.ocr.analyzer

import com.example.bowlingmaster200.domain.model.Frame
import com.example.bowlingmaster200.domain.model.GameMetadata
import com.example.bowlingmaster200.domain.model.Roll
import com.example.bowlingmaster200.domain.model.SavedGame
import com.example.bowlingmaster200.ocr.pipeline.OcrResult
import com.example.bowlingmaster200.ocr.service.OcrLogger
import com.example.bowlingmaster200.ocr.service.OcrSafeResults

/**
 * OCR 文字列から [SavedGame] を生成する責務。
 * 将来 SpareMaster の ScoreSheetLayout / SymbolRowAnalyzer ロジックを移植。
 */
class BowlingScoreSheetAnalyzer {

    fun analyze(ocrResult: OcrResult): ScoreSheetAnalysisResult {
        return try {
            analyzeInternal(ocrResult)
        } catch (error: Exception) {
            OcrLogger.e("BowlingScoreSheetAnalyzer failed", error)
            OcrSafeResults.safeAnalysis(
                ocrResult = ocrResult,
                warnings = listOf("Analyzer error: ${error.message ?: "unknown"}"),
            )
        }
    }

    private fun analyzeInternal(ocrResult: OcrResult): ScoreSheetAnalysisResult {
        val warnings = mutableListOf<String>()
        val rawText = ocrResult.rawText.orEmpty()
        val frameMap = parseFrameMap(rawText, warnings)
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
                try {
                    parseFrameLine(line, warnings)?.let { (frameIndex, frame) ->
                        frameMap[frameIndex] = frame
                    }
                } catch (error: Exception) {
                    OcrLogger.e("Skipped corrupt frame line: $line", error)
                    warnings.add("Skipped corrupt frame line: $line")
                }
            }

        if (frameMap.isEmpty() && rawText.isNotBlank()) {
            warnings.add("No frame lines parsed; using empty frames")
        }

        return frameMap
    }

    private fun parseFrameLine(line: String, warnings: MutableList<String>): Pair<Int, Frame>? {
        val colonIndex = line.indexOf(':')
        if (colonIndex <= 0) {
            warnings.add("Skipped invalid frame line: $line")
            return null
        }

        val framePart = line.substring(0, colonIndex).removePrefix("F").removePrefix("f")
        val frameIndex = framePart.trim().toIntOrNull()
        if (frameIndex == null || frameIndex !in 1..Frame.FRAME_COUNT) {
            warnings.add("Skipped invalid frame line: $line")
            OcrLogger.d("Abnormal frame index in line: $line")
            return null
        }

        val rolls = line.substring(colonIndex + 1).split(",")
            .map { it.trim().toIntOrNull() }

        rolls.forEachIndexed { rollIndex, value ->
            if (value != null && value !in Roll.MIN_PINS..Roll.MAX_PINS) {
                OcrLogger.d("Abnormal roll value F$frameIndex roll$rollIndex=$value in line: $line")
            }
        }

        val frame = Frame(
            firstRoll = rolls.getOrNull(0),
            secondRoll = rolls.getOrNull(1),
            bonusRoll = if (frameIndex == Frame.FRAME_COUNT) rolls.getOrNull(2) else null,
        )

        if (frame.firstRoll == null) {
            warnings.add("Skipped incomplete frame F$frameIndex")
            OcrLogger.d("Skipped incomplete frame F$frameIndex in line: $line")
            return null
        }

        if (!isValidFrameRolls(frameIndex, frame, warnings, line)) {
            return null
        }

        return frameIndex to frame
    }

    private fun isValidFrameRolls(
        frameIndex: Int,
        frame: Frame,
        warnings: MutableList<String>,
        line: String,
    ): Boolean {
        val first = frame.firstRoll ?: return false
        if (first !in Roll.MIN_PINS..Roll.MAX_PINS) {
            warnings.add("Skipped invalid roll range in line: $line")
            return false
        }

        if (frameIndex < Frame.FRAME_COUNT) {
            if (first == Roll.MAX_PINS && frame.secondRoll != null) {
                warnings.add("Skipped invalid strike follow-up in line: $line")
                OcrLogger.d("Invalid strike follow-up in line: $line")
                return false
            }
            frame.secondRoll?.let { second ->
                if (second !in Roll.MIN_PINS..Roll.MAX_PINS || first + second > Roll.FRAME_PINS) {
                    warnings.add("Skipped invalid roll sum in line: $line")
                    OcrLogger.d("Invalid roll sum in line: $line")
                    return false
                }
            }
        }

        return true
    }

    private fun normalizeFrames(frameMap: Map<Int, Frame>): List<Frame> {
        return (1..Frame.FRAME_COUNT).map { index ->
            frameMap[index] ?: Frame()
        }
    }
}
