package com.example.bowlingmaster200.ocr.analyzer

import com.example.bowlingmaster200.ocr.pipeline.OcrResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BowlingScoreSheetAnalyzerSafetyTest {

    private val analyzer = BowlingScoreSheetAnalyzer()

    @Test
    fun analyze_skipsIncompleteFrameLines() {
        val result = analyzer.analyze(
            OcrResult(
                rawText = "F1:10\nF2:\nF3:8,0",
                engineId = "test",
            ),
        )

        assertEquals(10, result.savedGame?.frames?.size)
        assertEquals(10, result.savedGame?.frames?.get(0)?.firstRoll)
        assertNull(result.savedGame?.frames?.get(1)?.firstRoll)
        assertEquals(8, result.savedGame?.frames?.get(2)?.firstRoll)
        assertTrue(result.warnings.any { it.contains("Skipped incomplete frame F2") })
    }

    @Test
    fun analyze_emptyRawText_returnsSafeEmptyGame() {
        val result = analyzer.analyze(
            OcrResult(
                rawText = "",
                engineId = "test",
            ),
        )

        assertEquals(10, result.savedGame?.frames?.size)
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test
    fun analyze_corruptLine_doesNotCrash() {
        val result = analyzer.analyze(
            OcrResult(
                rawText = "F1:10\nnot-a-frame\nF2:7,3",
                engineId = "test",
            ),
        )

        assertEquals(10, result.savedGame?.frames?.get(0)?.firstRoll)
        assertEquals(7, result.savedGame?.frames?.get(1)?.firstRoll)
    }
}
