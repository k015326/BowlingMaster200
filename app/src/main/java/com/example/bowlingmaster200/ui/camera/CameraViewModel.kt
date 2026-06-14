package com.example.bowlingmaster200.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bowlingmaster200.camera.CapturedCameraFrame
import com.example.bowlingmaster200.ocr.mapper.OcrInputMapper
import com.example.bowlingmaster200.ocr.pipeline.OcrPipeline
import com.example.bowlingmaster200.ocr.service.OcrLogger
import com.example.bowlingmaster200.ocr.service.OcrServiceFactory
import com.example.bowlingmaster200.ocr.usecase.ProcessOcrResultUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CameraViewModel : ViewModel() {

    private val pipeline = OcrPipeline.createDefault(
        ocrService = OcrServiceFactory.create(OcrServiceFactory.EngineMode.REAL),
    )
    private val processOcrResult = ProcessOcrResultUseCase()

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _scanGeneration = MutableStateFlow(0)
    val scanGeneration: StateFlow<Int> = _scanGeneration.asStateFlow()

    fun onFrameCaptured(frame: CapturedCameraFrame) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            try {
                val pipelineResult = pipeline.execute(
                    OcrInputMapper.fromCapturedFrame(frame, sourceLabel = "camera-live"),
                )
                val ocrResult = pipelineResult.ocrResult
                val uiResult = processOcrResult.execute(pipelineResult)

                val parsedFrames = pipelineResult.analysis.savedGame?.frames
                    ?.count { it.firstRoll != null }
                    ?: 0

                val frameDisplays = uiResult.gameScore?.frameScores?.map { score ->
                    OcrFrameDisplay(
                        frameIndex = score.frameIndex,
                        roll1 = formatOcrRoll(score.firstRoll),
                        roll2 = formatOcrRoll(
                            value = score.secondRoll,
                            isSecondRoll = true,
                            firstRoll = score.firstRoll,
                        ),
                        roll3 = score.bonusRoll?.let { formatOcrRoll(it) },
                        cumulative = score.cumulativeScore?.toString() ?: "—",
                    )
                } ?: emptyList()

                _uiState.update {
                    CameraUiState(
                        isProcessing = false,
                        rawOcrText = ocrResult.rawText.ifBlank { "(empty)" },
                        engineId = uiResult.engineId ?: ocrResult.engineId,
                        confidence = uiResult.confidence ?: ocrResult.confidence,
                        totalScore = uiResult.gameScore?.totalScore,
                        isScoreComplete = uiResult.gameScore?.isComplete == true,
                        parsedFrameCount = parsedFrames,
                        frameDisplays = frameDisplays,
                        warnings = uiResult.warnings,
                        errorMessage = uiResult.errorMessage,
                        isFallbackActive = ocrResult.debugInfo["fallback"] == "true",
                        fallbackReason = ocrResult.debugInfo["fallbackReason"],
                    )
                }
            } catch (error: Exception) {
                OcrLogger.e("Camera OCR pipeline failed", error)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = error.message ?: "OCR processing failed",
                    )
                }
            }
        }
    }

    fun requestRescan() {
        _scanGeneration.update { it + 1 }
        _uiState.update { CameraUiState(isProcessing = true) }
    }
}
