package com.example.bowlingmaster200.ocr.service

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
}
