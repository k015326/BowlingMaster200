package com.example.bowlingmaster200.ui.camera

import androidx.lifecycle.ViewModel
import com.example.bowlingmaster200.camera.CapturedCameraFrame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraViewModel : ViewModel() {

    private val _capturedFrame = MutableStateFlow<CapturedCameraFrame?>(null)
    val capturedFrame: StateFlow<CapturedCameraFrame?> = _capturedFrame.asStateFlow()

    fun onFrameCaptured(frame: CapturedCameraFrame) {
        _capturedFrame.value = frame
    }
}
