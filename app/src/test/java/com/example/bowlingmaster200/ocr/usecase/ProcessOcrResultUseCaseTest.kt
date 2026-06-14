package com.example.bowlingmaster200.ocr.usecase

import com.example.bowlingmaster200.ocr.pipeline.OcrImageSource
import com.example.bowlingmaster200.ocr.pipeline.OcrInput
import com.example.bowlingmaster200.ocr.pipeline.OcrInputMetadata
import com.example.bowlingmaster200.ocr.pipeline.OcrPipeline
import com.example.bowlingmaster200.ocr.service.FakeOcrService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessOcrResultUseCaseTest {

    @Test
    fun fakeOcrPipeline_produces300GameScore_forUi() = runBlocking {
        val pipeline = OcrPipeline.createDefault(FakeOcrService())
        val pipelineResult = pipeline.execute(
            OcrInput(
                source = OcrImageSource.Bytes(byteArrayOf(0), "image/jpeg"),
                metadata = OcrInputMetadata(sourceLabel = "test"),
            ),
        )

        val uiResult = ProcessOcrResultUseCase().execute(pipelineResult)

        assertTrue(uiResult.isSuccess)
        assertNull(uiResult.errorMessage)
        assertEquals(300, uiResult.gameScore?.totalScore)
        assertTrue(uiResult.gameScore?.isComplete == true)
        assertEquals(1.0f, uiResult.confidence)
        assertEquals(FakeOcrService.ENGINE_ID, uiResult.engineId)
        assertTrue(uiResult.warnings.isEmpty())
        assertEquals(10, uiResult.gameScore?.frameScores?.size)
    }
}
