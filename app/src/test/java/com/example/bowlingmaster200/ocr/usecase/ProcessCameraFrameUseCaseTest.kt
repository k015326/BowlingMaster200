package com.example.bowlingmaster200.ocr.usecase

import com.example.bowlingmaster200.ocr.camera.CameraFrame
import com.example.bowlingmaster200.ocr.camera.ImageFormat
import com.example.bowlingmaster200.ocr.service.FakeOcrService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessCameraFrameUseCaseTest {

    @Test
    fun cameraFrame_fullPath_reachesCalculatorWith300() = runBlocking {
        val uiResult = ProcessCameraFrameUseCase().execute(sampleCameraFrame())

        assertTrue(uiResult.isSuccess)
        assertNull(uiResult.errorMessage)
        assertEquals(300, uiResult.gameScore?.totalScore)
        assertEquals(FakeOcrService.ENGINE_ID, uiResult.engineId)
        assertEquals(10, uiResult.gameScore?.frameScores?.size)
    }

    private fun sampleCameraFrame(): CameraFrame {
        return CameraFrame(
            imageBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()),
            width = 640,
            height = 480,
            rotationDegrees = 0,
            format = ImageFormat.JPEG,
        )
    }
}
