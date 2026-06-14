package com.example.bowlingmaster200.ocr.mapper

import com.example.bowlingmaster200.domain.calculator.BowlingScoreCalculator
import com.example.bowlingmaster200.domain.model.Frame
import com.example.bowlingmaster200.ocr.analyzer.ScoreSheetAnalysisResult
import com.example.bowlingmaster200.ocr.camera.CameraFrame
import com.example.bowlingmaster200.ocr.camera.ImageFormat
import com.example.bowlingmaster200.ocr.pipeline.OcrImageSource
import com.example.bowlingmaster200.ocr.pipeline.OcrInput
import com.example.bowlingmaster200.ocr.pipeline.OcrPipeline
import com.example.bowlingmaster200.ocr.service.FakeOcrService
import com.example.bowlingmaster200.ocr.service.OcrService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrInputMapperPipelineTest {

    @Test
    fun fromCameraFrame_mapsToOcrInputWithCameraSource() {
        val cameraFrame = sampleCameraFrame()

        val ocrInput = OcrInputMapper.fromCameraFrame(cameraFrame)

        val source = ocrInput.source as OcrImageSource.Camera
        assertEquals(cameraFrame, source.frame)
        assertEquals("camera", ocrInput.metadata.sourceLabel)
        assertEquals(90, ocrInput.metadata.rotationDegrees)
    }

    @Test
    fun cameraFrame_throughMapper_fakeOcr_reachesAnalyzer() = runBlocking {
        val pipeline = OcrPipeline.createDefault(FakeOcrService())
        val cameraFrame = sampleCameraFrame()

        val result = pipeline.execute(OcrInputMapper.fromCameraFrame(cameraFrame))

        assertOcrPipelineResult(result.ocrResult.engineId, result.analysis)
    }

    @Test
    fun executeFromCamera_usesMapper_withFakeOcrService() = runBlocking {
        val pipeline = OcrPipeline.createDefault(FakeOcrService())
        val cameraFrame = sampleCameraFrame()

        val result = pipeline.executeFromCamera(cameraFrame)

        assertOcrPipelineResult(result.ocrResult.engineId, result.analysis)
        val source = result.input.source as OcrImageSource.Camera
        assertEquals(cameraFrame, source.frame)
    }

    @Test
    fun ocrService_isReplaceable_viaOcrInput() = runBlocking {
        val customService = object : OcrService {
            override val engineId: String = "custom-stub"

            override suspend fun recognize(input: OcrInput) =
                FakeOcrService().recognize(input).copy(engineId = engineId)
        }
        val pipeline = OcrPipeline.createDefault(customService)
        val result = pipeline.executeFromCamera(sampleCameraFrame())

        assertEquals("custom-stub", result.ocrResult.engineId)
        assertNotNull(result.analysis.savedGame)
    }

    private fun assertOcrPipelineResult(
        engineId: String,
        analysis: ScoreSheetAnalysisResult,
    ) {
        assertEquals(FakeOcrService.ENGINE_ID, engineId)
        assertNotNull(analysis.savedGame)
        assertEquals(Frame.FRAME_COUNT, analysis.savedGame?.frames?.size)
        assertEquals(
            300,
            analysis.savedGame?.frames?.let { frames ->
                BowlingScoreCalculator.calculateTotalScore(frames)
            },
        )
        assertTrue(analysis.warnings.isEmpty())
    }

    private fun sampleCameraFrame(): CameraFrame {
        return CameraFrame(
            imageBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()),
            width = 640,
            height = 480,
            rotationDegrees = 90,
            format = ImageFormat.JPEG,
        )
    }
}
