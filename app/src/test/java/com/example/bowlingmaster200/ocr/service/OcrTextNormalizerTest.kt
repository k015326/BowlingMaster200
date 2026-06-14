package com.example.bowlingmaster200.ocr.service

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
        assertEquals("F4:10", OcrTextNormalizer.normalizeLine("F4:X"))
        assertEquals("F5:0,1", OcrTextNormalizer.normalizeLine("F5:-,l"))
    }

    @Test
    fun normalizeLine_fixesFrameNumberMisreads() {
        assertEquals("F1:10", OcrTextNormalizer.normalizeLine("Fl:10"))
        assertEquals("F10:10,10,10", OcrTextNormalizer.normalizeLine("F1O:10,10,10"))
    }

    @Test
    fun splitIntoFrameLines_splitsMergedRow() {
        val split = OcrTextNormalizer.splitIntoFrameLines("F1:10 F2:7,3 F3:8,0")
        assertEquals(listOf("F1:10", "F2:7,3", "F3:8,0"), split)
    }

    @Test
    fun normalizeLine_emptyInput_returnsEmpty() {
        assertEquals("", OcrTextNormalizer.normalizeLine("   "))
    }

    @Test
    fun isUsableForAnalyzer_requiresValidFrameLine() {
        assertTrue(OcrTextNormalizer.isUsableForAnalyzer("Header\nF1:10\nF2:7,3"))
        assertFalse(OcrTextNormalizer.isUsableForAnalyzer("Bowling Score Sheet"))
        assertFalse(OcrTextNormalizer.isUsableForAnalyzer(""))
    }
}

class OcrAnalyzerInputFilterTest {

    @Test
    fun filterRawText_keepsValidFramesSorted() {
        val result = OcrAnalyzerInputFilter.filterRawText(
            """
            Bowling Score
            F3:8,0
            F1:10
            F2:7,3
            """.trimIndent(),
        )

        assertEquals(
            listOf("F1:10", "F2:7,3", "F3:8,0"),
            result.acceptedLines,
        )
        assertEquals("F1:10\nF2:7,3\nF3:8,0", result.rawText)
    }

    @Test
    fun filterRawText_rejectsInvalidRolls() {
        val result = OcrAnalyzerInputFilter.filterRawText("F1:11\nF2:7,3")

        assertEquals(listOf("F2:7,3"), result.acceptedLines)
        assertTrue(result.rejectedLines.isNotEmpty())
    }

    @Test
    fun filterRawText_rejectsStrikeWithSecondRollInRegularFrame() {
        val result = OcrAnalyzerInputFilter.filterRawText("F1:10,5\nF2:8,0")

        assertEquals(listOf("F2:8,0"), result.acceptedLines)
    }

    @Test
    fun filterRawText_emptyInput_returnsEmpty() {
        val result = OcrAnalyzerInputFilter.filterRawText("  \n  ")

        assertEquals("", result.rawText)
        assertTrue(result.acceptedLines.isEmpty())
    }

    @Test
    fun filterRawText_deduplicatesByFrameKeepingLast() {
        val result = OcrAnalyzerInputFilter.filterRawText("F1:9,0\nF1:10")

        assertEquals(listOf("F1:10"), result.acceptedLines)
    }
}

class FallbackOcrServiceTest {

    @Test
    fun recognize_primaryFailure_fallsBackToFake() = kotlinx.coroutines.runBlocking {
        val failingPrimary = object : OcrEngine {
            override val engineId: String = "failing"

            override suspend fun recognize(input: com.example.bowlingmaster200.ocr.pipeline.OcrInput): OcrResult {
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
    fun recognize_emptyPrimary_fallsBackToFake() = kotlinx.coroutines.runBlocking {
        val emptyPrimary = object : OcrEngine {
            override val engineId: String = MlKitOcrService.ENGINE_ID

            override suspend fun recognize(input: com.example.bowlingmaster200.ocr.pipeline.OcrInput): OcrResult {
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
    fun recognize_usablePrimary_keepsPrimaryResult() = kotlinx.coroutines.runBlocking {
        val primaryResult = OcrResult(
            rawText = "F1:10\nF2:7,3",
            engineId = MlKitOcrService.ENGINE_ID,
        )
        val primary = object : OcrEngine {
            override val engineId: String = MlKitOcrService.ENGINE_ID

            override suspend fun recognize(input: com.example.bowlingmaster200.ocr.pipeline.OcrInput): OcrResult =
                primaryResult
        }
        val service = FallbackOcrService(primary, FakeOcrService())

        val result = service.recognize(testInput())

        assertEquals(MlKitOcrService.ENGINE_ID, result.engineId)
        assertFalse(result.debugInfo.containsKey("fallback"))
        assertEquals(primaryResult.rawText, result.rawText)
    }

    private fun testInput(): com.example.bowlingmaster200.ocr.pipeline.OcrInput {
        return com.example.bowlingmaster200.ocr.pipeline.OcrInput(
            source = com.example.bowlingmaster200.ocr.pipeline.OcrImageSource.Bytes(
                byteArrayOf(0),
                "image/jpeg",
            ),
            metadata = com.example.bowlingmaster200.ocr.pipeline.OcrInputMetadata(sourceLabel = "test"),
        )
    }
}
