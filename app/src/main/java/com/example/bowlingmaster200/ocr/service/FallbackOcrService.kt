package com.example.bowlingmaster200.ocr.service

import com.example.bowlingmaster200.ocr.pipeline.OcrInput
import com.example.bowlingmaster200.ocr.pipeline.OcrResult

/**
 * プライマリ OCR 失敗時に [FakeOcrService] へフォールバックする [OcrEngine]。
 */
class FallbackOcrService(
    private val primary: OcrEngine,
    private val fallback: OcrEngine = FakeOcrService(),
) : OcrEngine {

    override val engineId: String = primary.engineId

    override suspend fun recognize(input: OcrInput): OcrResult {
        return try {
            val result = primary.recognize(input)
            if (shouldFallback(result)) {
                recognizeWithFallback(input, reason = "empty_or_unparseable", primaryResult = result)
            } else {
                result
            }
        } catch (error: Exception) {
            OcrLogger.e("Primary OCR failed, switching to fallback", error)
            recognizeWithFallback(input, reason = error.message ?: "error")
        }
    }

    private suspend fun recognizeWithFallback(
        input: OcrInput,
        reason: String,
        primaryResult: OcrResult? = null,
    ): OcrResult {
        OcrLogger.d("Fallback to FakeOcrService: reason=$reason")
        val fallbackResult = fallback.recognize(input)
        val mergedDebug = buildMap {
            put("fallback", "true")
            put("fallbackReason", reason)
            put("primaryEngine", primary.engineId)
            primaryResult?.debugInfo?.forEach { (key, value) ->
                put("primary_$key", value)
            }
            putAll(fallbackResult.debugInfo)
        }
        return fallbackResult.copy(
            debugInfo = mergedDebug,
        ).also { OcrLogger.logOcrResult(it) }
    }

    private fun shouldFallback(result: OcrResult): Boolean {
        if (result.rawText.isBlank()) return true
        if (result.lines.isEmpty()) return true
        return !OcrTextNormalizer.isUsableForAnalyzer(result.rawText)
    }
}
