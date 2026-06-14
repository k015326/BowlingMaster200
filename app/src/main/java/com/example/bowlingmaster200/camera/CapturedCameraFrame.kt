package com.example.bowlingmaster200.camera

import android.graphics.Bitmap

/**
 * CameraX から取得した1フレーム（Bitmap）。
 */
data class CapturedCameraFrame(
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
)
