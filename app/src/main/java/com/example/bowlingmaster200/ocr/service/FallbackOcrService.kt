package com.example.bowlingmaster200.ocr.service

import com.example.bowlingmaster200.ocr.pipeline.OcrInput
import com.example.bowlingmaster200.ocr.pipeline.OcrResult

/**
 * プライマリ OCR 失敗時にフォールバック OCR へ切り替える [OcrEngine]。
 */
class FallbackOcrService(
    private val primary: OcrEngine,
    private val fallback: OcrEngine,
) : OcrEngine {

    override val engineId: String = primary.engineId

    override suspend fun recognize(input: OcrInput): OcrResult {
        return try {
            val result = primary.recognize(input)
            if (shouldFallback(result)) {
                val reason = resolveFallbackReason(result)
                OcrLogger.logFallbackReason(
                    reason = reason,
                    primaryEngine = result.engineId,
                    primaryLineCount = result.lines.size,
                    primaryRawText = result.rawText,
                )
                recognizeWithFallback(input, reason = reason, primaryResult = result)
            } else {
                result
            }
        } catch (error: Exception) {
            OcrLogger.e("Primary OCR failed, switching to fallback", error)
            OcrLogger.logFallbackReason(
                reason = REASON_OCR_EXCEPTION,
                primaryEngine = primary.engineId,
                primaryLineCount = null,
                primaryRawText = null,
                exception = error,
            )
            recognizeWithFallback(input, reason = REASON_OCR_EXCEPTION)
        }
    }

    private suspend fun recognizeWithFallback(
        input: OcrInput,
        reason: String,
        primaryResult: OcrResult? = null,
    ): OcrResult {
        OcrLogger.d("Fallback OCR engaged: reason=$reason")
        return try {
            val fallbackResult = fallback.recognize(input)
            val mergedDebug = buildMap {
                put("fallback", "true")
                put("fallbackReason", reason)
                put("primaryEngine", primary.engineId)
                primaryResult?.let { primary ->
                    put("primaryRawText", truncateForDebug(primary.rawText))
                    put("primaryLineCount", primary.lines.size.toString())
                    put(
                        "primary_usable",
                        OcrTextNormalizer.isUsableForAnalyzer(primary.rawText).toString(),
                    )
                    primary.debugInfo.forEach { (key, value) ->
                        put("primary_$key", value)
                    }
                }
                putAll(fallbackResult.debugInfo)
            }
            fallbackResult.copy(
                debugInfo = mergedDebug,
            ).also { OcrLogger.logOcrResult(it) }
        } catch (error: Exception) {
            OcrLogger.e("Fallback OCR also failed", error)
            OcrSafeResults.emptyOcrResult(
                engineId = fallback.engineId,
                reason = "fallback_failed:${error.message}",
            ).copy(
                debugInfo = mapOf(
                    "fallback" to "true",
                    "fallbackReason" to reason,
                    "primaryEngine" to primary.engineId,
                    "fallbackError" to (error.message ?: "unknown"),
                ),
            )
        }
    }

    private fun shouldFallback(result: OcrResult): Boolean {
        if (result.rawText.isBlank()) return true
        if (result.lines.isEmpty()) return true
        return !OcrTextNormalizer.isUsableForAnalyzer(result.rawText)
    }

    internal fun resolveFallbackReason(result: OcrResult): String {
        if (result.rawText.isBlank()) return REASON_EMPTY_TEXT
        if (result.lines.isEmpty()) return REASON_NO_LINES_DETECTED
        if (!OcrTextNormalizer.isUsableForAnalyzer(result.rawText)) return REASON_PARSE_FAILED
        return REASON_UNKNOWN
    }

    private fun truncateForDebug(text: String, maxLength: Int = MAX_DEBUG_TEXT_LENGTH): String {
        if (text.length <= maxLength) return text
        return text.take(maxLength) + "…(${text.length} chars total)"
    }

    companion object {
        const val REASON_EMPTY_TEXT = "empty_text"
        const val REASON_NO_LINES_DETECTED = "no_lines_detected"
        const val REASON_PARSE_FAILED = "parse_failed"
        const val REASON_OCR_EXCEPTION = "ocr_exception"
        const val REASON_UNKNOWN = "unknown"

        private const val MAX_DEBUG_TEXT_LENGTH = 4000
    }
}
