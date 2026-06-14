package com.example.bowlingmaster200.ocr.service

/**
 * Analyzer 投入前のフレーム行フィルタ（不正行除外・F1〜F10 整形）。
 * [BowlingScoreSheetAnalyzer] は変更せず、OCR 出力のみを整える。
 */
internal object OcrAnalyzerInputFilter {

    private val FRAME_LINE_REGEX = Regex(
        """^F(\d{1,2}):(.+)$""",
        RegexOption.IGNORE_CASE,
    )

    private val HEADER_KEYWORDS = setOf(
        "bowling",
        "score",
        "sheet",
        "frame",
        "total",
        "player",
    )

    data class FilterResult(
        val rawText: String,
        val acceptedLines: List<String>,
        val rejectedLines: List<String>,
    )

    fun filterLines(lines: List<String>): FilterResult {
        if (lines.isEmpty()) {
            return FilterResult(rawText = "", acceptedLines = emptyList(), rejectedLines = emptyList())
        }

        val acceptedByFrame = linkedMapOf<Int, String>()
        val rejected = mutableListOf<String>()

        lines.flatMap { line -> OcrTextNormalizer.splitIntoFrameLines(line) }
            .forEach { candidate ->
                val normalized = OcrTextNormalizer.normalizeLine(candidate)
                if (normalized.isEmpty()) return@forEach

                val parsed = parseValidFrameLine(normalized)
                when {
                    parsed != null -> acceptedByFrame[parsed.frameIndex] = parsed.formattedLine
                    isIgnorableLine(normalized) -> rejected.add(normalized)
                    else -> rejected.add(normalized)
                }
            }

        val accepted = (1..10).mapNotNull { index -> acceptedByFrame[index] }
        return FilterResult(
            rawText = accepted.joinToString("\n"),
            acceptedLines = accepted,
            rejectedLines = rejected,
        )
    }

    fun filterRawText(rawText: String): FilterResult {
        if (rawText.isBlank()) {
            return FilterResult(rawText = "", acceptedLines = emptyList(), rejectedLines = emptyList())
        }
        return filterLines(rawText.lines())
    }

    private data class ParsedFrameLine(
        val frameIndex: Int,
        val formattedLine: String,
    )

    private fun parseValidFrameLine(line: String): ParsedFrameLine? {
        val match = FRAME_LINE_REGEX.matchEntire(line) ?: return null
        val frameIndex = match.groupValues[1].toIntOrNull() ?: return null
        if (frameIndex !in 1..10) return null

        val rollValues = match.groupValues[2]
            .split(",")
            .mapNotNull { segment -> OcrTextNormalizer.parseRollValue(segment) }

        if (rollValues.isEmpty()) return null

        val maxRolls = if (frameIndex == 10) 3 else 2
        val rolls = rollValues.take(maxRolls)
        if (!isValidRollSequence(frameIndex, rolls)) return null

        return ParsedFrameLine(
            frameIndex = frameIndex,
            formattedLine = "F$frameIndex:${rolls.joinToString(",")}",
        )
    }

    private fun isValidRollSequence(frameIndex: Int, rolls: List<Int>): Boolean {
        if (rolls.isEmpty()) return false
        rolls.forEach { if (it !in 0..10) return false }

        val first = rolls[0]
        if (frameIndex < 10 && first == 10 && rolls.size > 1) return false
        if (rolls.size >= 2 && frameIndex < 10) {
            val second = rolls[1]
            if (first != 10 && first + second > 10) return false
        }
        return true
    }

    private fun isIgnorableLine(line: String): Boolean {
        if (!line.contains(':')) {
            val lower = line.lowercase()
            return HEADER_KEYWORDS.any { lower.contains(it) }
        }
        return false
    }
}
