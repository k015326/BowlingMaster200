package com.example.bowlingmaster200.ocr.pipeline

import com.example.bowlingmaster200.ocr.camera.CameraFrame
import com.example.bowlingmaster200.ocr.camera.ImageFormat
import com.example.bowlingmaster200.ocr.service.OcrEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrPipelineSafetyTest {

    @Test
    fun execute_ocrFailure_returnsSafeResultWithoutCrash() = runBlocking {
        val pipeline = OcrPipeline(
            ocrService = object : OcrEngine {
                override val engineId: String = "failing"

                override suspend fun recognize(input: OcrInput): OcrResult {
                    error("OCR exploded")
                }
            },
        )

        val result = pipeline.execute(
            OcrInput(
                source = OcrImageSource.Bytes(byteArrayOf(1), "image/jpeg"),
            ),
        )

        assertNotNull(result.analysis.savedGame)
        assertEquals("", result.ocrResult.rawText)
        assertTrue(result.analysis.warnings.isNotEmpty())
    }

    @Test
    fun executeFromCamera_emptyBytes_returnsSafeResultWithoutCrash() = runBlocking {
        val pipeline = OcrPipeline.createDefault()

        val result = pipeline.executeFromCamera(
            CameraFrame(
                imageBytes = byteArrayOf(),
                width = 0,
                height = 0,
                format = ImageFormat.JPEG,
            ),
        )

        assertNotNull(result.analysis.savedGame)
        assertEquals("", result.ocrResult.rawText)
        assertTrue(result.analysis.warnings.any { it.contains("empty_camera_frame") })
    }
}
