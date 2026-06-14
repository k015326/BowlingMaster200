package com.example.bowlingmaster200.ui.camera

data class CameraUiState(
    val isProcessing: Boolean = false,
    val rawOcrText: String? = null,
    val engineId: String? = null,
    val confidence: Float? = null,
    val totalScore: Int? = null,
    val isScoreComplete: Boolean = false,
    val parsedFrameCount: Int = 0,
    val frameDisplays: List<OcrFrameDisplay> = emptyList(),
    val warnings: List<String> = emptyList(),
    val errorMessage: String? = null,
    val isFallbackActive: Boolean = false,
    val fallbackReason: String? = null,
    val debugInfo: Map<String, String> = emptyMap(),
)
