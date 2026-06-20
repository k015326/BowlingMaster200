package com.example.bowlingmaster200.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.bowlingmaster200.ocr.image.CaptureGuideFrame

/**
 * カメラプレビュー上の1ゲーム撮影ガイド（SpareMaster 移植）。
 */
@Composable
fun CaptureGuideOverlay(
    frameReady: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val guideRect = CaptureGuideFrame.toComposeRect(size.width, size.height)
        val overlayPath = Path().apply {
            addRect(Rect(Offset.Zero, size))
            addRect(guideRect)
            fillType = PathFillType.EvenOdd
        }
        drawPath(overlayPath, Color.Black.copy(alpha = 0.62f))
        val borderColor = if (frameReady) Color(0xFF4CAF50) else Color.Red
        drawRect(
            color = borderColor,
            topLeft = guideRect.topLeft,
            size = Size(guideRect.width, guideRect.height),
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}
