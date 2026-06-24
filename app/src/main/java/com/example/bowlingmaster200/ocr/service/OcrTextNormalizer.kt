package com.example.bowlingmaster200.ocr.service

import com.google.mlkit.vision.text.Text

/**
 * ML Kit 出力の正規化（行整形・数字誤認識補正・F1〜F10 フォーマット補正）。
 */
internal object OcrTextNormalizer {

    private val FRAME_LINE_PATTERN = Regex("""^F(\d{1,2}):.+$""", RegexOption.IGNORE_CASE)
    private val FRAME_SPLIT_PATTERN = Regex("""(?=F\s*\d)""", RegexOption.IGNORE_CASE)
    private val FRAME_PREFIX_PATTERN = Regex("""^[Ff]\s*([ilILoO0|1-9]\d?)\s*[:;,]\s*(.*)$""")

    data class NormalizedText(
        val rawText: String,
        val lines: List<String>,
        val blockCount: Int,
        val droppedLineCount: Int,
        val filteredLineCount: Int = 0,
        val rejectedLineCount: Int = 0,
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

        OcrLogger.logMlKitSortedLines(sortedLines)
        sortedLines.forEach { line ->
            OcrLogger.logLineSplitDebug(
                sourceText = line.text,
                sourceBbox = line.boundingBox,
                parts = splitIntoFrameLines(line.text),
            )
        }

        var dropped = 0
        val candidateLines = sortedLines.flatMap { line ->
            splitIntoFrameLines(line.text)
        }.mapNotNull { candidate ->
            val normalized = normalizeLine(candidate)
            if (normalized.isEmpty()) {
                dropped++
                null
            } else {
                normalized
            }
        }

        OcrLogger.logNormalizationCandidates(candidateLines)
        OcrLogger.logFilterLinesBoundary("before", candidateLines)
        val filtered = OcrAnalyzerInputFilter.filterLines(candidateLines)
        OcrLogger.logFilterLinesAfter(filtered)
        OcrLogger.logFilterPipelineSummary(
            mlKitTextLength = visionText.text.length,
            blockCount = visionText.textBlocks.size,
            sortedLineCount = sortedLines.size,
            droppedLineCount = dropped,
            candidateLineCount = candidateLines.size,
            acceptedLineCount = filtered.acceptedLines.size,
            rejectedLineCount = filtered.rejectedLines.size,
            filteredRawTextLength = filtered.rawText.length,
        )

        return NormalizedText(
            rawText = filtered.rawText,
            lines = filtered.acceptedLines,
            blockCount = visionText.textBlocks.size,
            droppedLineCount = dropped,
            filteredLineCount = filtered.acceptedLines.size,
            rejectedLineCount = filtered.rejectedLines.size,
        )
    }

    fun splitIntoFrameLines(text: String): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        val parts = FRAME_SPLIT_PATTERN.split(trimmed)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return if (parts.size <= 1) listOf(trimmed) else parts
    }

    fun normalizeLine(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ""

        val collapsed = trimmed
            .replace(Regex("\\s+"), " ")
            .replace('：', ':')
            .replace('；', ';')

        return normalizeFrameLine(collapsed)
    }

    fun parseRollValue(segment: String): Int? {
        val cleaned = normalizeRollSegment(segment)
        if (cleaned.isEmpty()) return null
        return cleaned.toIntOrNull()?.takeIf { it in 0..10 }
    }

    fun isUsableForAnalyzer(rawText: String): Boolean {
        if (rawText.isBlank()) return false
        return OcrAnalyzerInputFilter.filterRawText(rawText).acceptedLines.isNotEmpty()
    }

    private fun normalizeFrameLine(line: String): String {
        val frameMatch = FRAME_PREFIX_PATTERN.find(line) ?: return line

        val frameNumber = normalizeFrameNumber(frameMatch.groupValues[1]) ?: return line

        val rollsPart = frameMatch.groupValues[2]
            .replace(';', ',')
            .replace(Regex("\\s*,\\s*"), ",")
            .replace(Regex("\\s+"), "")

        val normalizedRolls = rollsPart.split(",")
            .filter { it.isNotEmpty() }
            .mapNotNull { parseRollValue(it) }

        if (normalizedRolls.isEmpty()) {
            return "F$frameNumber:"
        }

        return "F$frameNumber:${normalizedRolls.joinToString(",")}"
    }

    private fun normalizeFrameNumber(raw: String): String? {
        val corrected = raw
            .replace('l', '1')
            .replace('I', '1')
            .replace('i', '1')
            .replace('|', '1')
            .replace('O', '0')
            .replace('o', '0')

        val frameIndex = corrected.toIntOrNull() ?: return null
        if (frameIndex !in 1..10) return null
        return frameIndex.toString()
    }

    internal fun normalizeRollSegment(segment: String): String {
        val trimmed = segment.trim()
        if (trimmed.isEmpty()) return ""

        when {
            trimmed.equals("X", ignoreCase = true) -> return "10"
            trimmed == "-" || trimmed == "G" || trimmed.equals("gutter", ignoreCase = true) -> return "0"
        }

        return trimmed
            .replace('O', '0')
            .replace('o', '0')
            .replace('l', '1')
            .replace('I', '1')
            .replace('S', '5')
            .replace('s', '5')
            .replace('B', '8')
            .replace('b', '8')
            .filter { it.isDigit() }
    }
}
