package com.example.bowlingmaster200.ocr.service

import com.example.bowlingmaster200.ocr.pipeline.OcrImageSource
import com.example.bowlingmaster200.ocr.pipeline.OcrInput
import com.example.bowlingmaster200.ocr.pipeline.OcrInputMetadata
import com.example.bowlingmaster200.ocr.pipeline.OcrResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrTextNormalizerTest {

    @Test
    fun normalizeLine_collapsesWhitespaceAndFrameFormat() {
        assertEquals("F1:10,3", OcrTextNormalizer.normalizeLine("F 1 : 10 , 3"))
        assertEquals("F2:7,3", OcrTextNormalizer.normalizeLine("F2;7;3"))
    }

    @Test
    fun normalizeLine_fixesCommonMisreadsInRolls() {
        assertEquals("F3:8,0", OcrTextNormalizer.normalizeLine("F3:8,O"))
    }

    @Test
    fun normalizeLine_emptyInput_returnsEmpty() {
        assertEquals("", OcrTextNormalizer.normalizeLine("   "))
    }

    @Test
    fun isUsableForAnalyzer_requiresFrameLine() {
        assertTrue(OcrTextNormalizer.isUsableForAnalyzer("Header\nF1:10\nF2:7,3"))
        assertFalse(OcrTextNormalizer.isUsableForAnalyzer("Bowling Score Sheet"))
        assertFalse(OcrTextNormalizer.isUsableForAnalyzer(""))
    }
}

class FallbackOcrServiceTest {

    @Test
    fun recognize_primaryFailure_fallsBackToFake() = runBlocking {
        val failingPrimary = object : OcrEngine {
            override val engineId: String = "failing"

            override suspend fun recognize(input: OcrInput): OcrResult {
                error("ML Kit unavailable")
            }
        }
        val service = FallbackOcrService(failingPrimary, FakeOcrService())

        val result = service.recognize(testInput())

        assertEquals(FakeOcrService.ENGINE_ID, result.engineId)
        assertEquals("true", result.debugInfo["fallback"])
        assertTrue(result.rawText.contains("F1:10"))
    }

    @Test
    fun recognize_emptyPrimary_fallsBackToFake() = runBlocking {
        val emptyPrimary = object : OcrEngine {
            override val engineId: String = MlKitOcrService.ENGINE_ID

            override suspend fun recognize(input: OcrInput): OcrResult {
                return OcrResult(
                    rawText = "",
                    lines = emptyList(),
                    engineId = engineId,
                )
            }
        }
        val service = FallbackOcrService(emptyPrimary, FakeOcrService())

        val result = service.recognize(testInput())

        assertEquals(FakeOcrService.ENGINE_ID, result.engineId)
        assertEquals("true", result.debugInfo["fallback"])
    }

    @Test
    fun recognize_usablePrimary_keepsPrimaryResult() = runBlocking {
        val primaryResult = OcrResult(
            rawText = "F1:10\nF2:7,3",
            engineId = MlKitOcrService.ENGINE_ID,
        )
        val primary = object : OcrEngine {
            override val engineId: String = MlKitOcrService.ENGINE_ID

            override suspend fun recognize(input: OcrInput): OcrResult = primaryResult
        }
        val service = FallbackOcrService(primary, FakeOcrService())

        val result = service.recognize(testInput())

        assertEquals(MlKitOcrService.ENGINE_ID, result.engineId)
        assertFalse(result.debugInfo.containsKey("fallback"))
        assertEquals(primaryResult.rawText, result.rawText)
    }

    private fun testInput(): OcrInput {
        return OcrInput(
            source = OcrImageSource.Bytes(byteArrayOf(0), "image/jpeg"),
            metadata = OcrInputMetadata(sourceLabel = "test"),
        )
    }
}
