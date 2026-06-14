package com.example.bowlingmaster200.ocr.service

import com.example.bowlingmaster200.ocr.pipeline.OcrLine
import com.google.mlkit.vision.text.Text

/**
 * ML Kit 出力の最小正規化（テキスト結合・空行除去・フレーム行の安全補正）。
 */
internal object OcrTextNormalizer {

    private val FRAME_LINE_PATTERN = Regex("""^F(\d{1,2}):.+$""", RegexOption.IGNORE_CASE)

    data class NormalizedText(
        val rawText: String,
        val lines: List<OcrLine>,
        val blockCount: Int,
        val droppedLineCount: Int,
    )

    fun fromMlKitText(visionText: Text): NormalizedText {
        val sortedLines = visionText.textBlocks
            .flatMap { block -> block.lines }
            .sortedWith(
                compareBy(
                    { line -> line.boundingBox?.top ?: Int.MAX_VALUE },
                    { line -> line.boundingBox?.left ?: Int.MAX_VALUE },
                ),
            )

        var dropped = 0
        val normalizedLines = sortedLines.mapNotNull { line ->
            val normalized = normalizeLine(line.text)
            if (normalized.isEmpty()) {
                dropped++
                null
            } else {
                normalized
            }
        }

        val ocrLines = normalizedLines.mapIndexed { index, text ->
            OcrLine(text = text, confidence = null, lineIndex = index)
        }

        return NormalizedText(
            rawText = normalizedLines.joinToString("\n"),
            lines = ocrLines,
            blockCount = visionText.textBlocks.size,
            droppedLineCount = dropped,
        )
    }

    fun normalizeLine(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ""

        val collapsed = trimmed.replace(Regex("\\s+"), " ")
        return normalizeFrameLine(collapsed)
    }

    fun isUsableForAnalyzer(rawText: String): Boolean {
        if (rawText.isBlank()) return false
        return rawText.lines().any { line ->
            FRAME_LINE_PATTERN.matches(line.trim())
        }
    }

    private fun normalizeFrameLine(line: String): String {
        val frameMatch = Regex("""^[Ff]\s*([ilIL|1-9]\d?)\s*[:;,]\s*(.*)$""").find(line)
            ?: return line

        val frameNumber = frameMatch.groupValues[1]
            .replace('l', '1')
            .replace('I', '1')
            .replace('i', '1')
            .replace('|', '1')
            .trim()

        if (frameNumber.toIntOrNull() == null) return line

        val rollsPart = frameMatch.groupValues[2]
            .replace(';', ',')
            .replace(Regex("\\s*,\\s*"), ",")
            .replace(Regex("\\s+"), "")

        val normalizedRolls = rollsPart.split(",")
            .filter { it.isNotEmpty() }
            .joinToString(",") { segment -> normalizeRollSegment(segment) }

        return if (normalizedRolls.isEmpty()) {
            "F$frameNumber:"
        } else {
            "F$frameNumber:$normalizedRolls"
        }
    }

    private fun normalizeRollSegment(segment: String): String {
        return segment
            .replace('O', '0')
            .replace('o', '0')
            .replace('l', '1')
            .filter { it.isDigit() }
    }
}
