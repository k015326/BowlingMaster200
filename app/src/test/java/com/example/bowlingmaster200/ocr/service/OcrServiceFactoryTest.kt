package com.example.bowlingmaster200.ocr.service

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrServiceFactoryTest {

    @Test
    fun create_fakeMode_returnsFakeOcrService() {
        val engine = OcrServiceFactory.create(OcrServiceFactory.EngineMode.FAKE)

        assertEquals(FakeOcrService.ENGINE_ID, engine.engineId)
        assertTrue(engine is FakeOcrService)
    }

    @Test
    fun create_default_isFakeMode() {
        assertEquals(FakeOcrService.ENGINE_ID, OcrServiceFactory.create().engineId)
    }

    @Test
    fun create_realMode_returnsMlKitOcrService() {
        val context = mockk<Context>()
        every { context.applicationContext } returns context
        OcrServiceFactory.init(context)

        val engine = OcrServiceFactory.create(OcrServiceFactory.EngineMode.REAL)

        assertEquals(MlKitOcrService.ENGINE_ID, engine.engineId)
        assertTrue(engine is MlKitOcrService)
    }
}
