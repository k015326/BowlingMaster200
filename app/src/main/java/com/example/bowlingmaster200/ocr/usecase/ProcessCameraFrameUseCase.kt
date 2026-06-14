package com.example.bowlingmaster200.ocr.usecase

import com.example.bowlingmaster200.ocr.adapter.OcrProcessUiResult
import com.example.bowlingmaster200.ocr.camera.CameraFrame
import com.example.bowlingmaster200.ocr.pipeline.OcrPipeline

/**
 * CameraFrame → Pipeline → Analyzer → Calculator の完全経路。
 */
class ProcessCameraFrameUseCase(
    private val pipeline: OcrPipeline = OcrPipeline.createDefault(),
    private val processOcrResult: ProcessOcrResultUseCase = ProcessOcrResultUseCase(),
) {

    suspend fun execute(frame: CameraFrame): OcrProcessUiResult {
        val pipelineResult = pipeline.executeFromCamera(frame)
        return processOcrResult.execute(pipelineResult)
    }
}
