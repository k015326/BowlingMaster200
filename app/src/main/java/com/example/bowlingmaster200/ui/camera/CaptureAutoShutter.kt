package com.example.bowlingmaster200.ui.camera

import android.graphics.ImageFormat
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.bowlingmaster200.ocr.image.CaptureGuideFrame
import kotlin.math.abs

data class CaptureAutoShutterState(
    val positioned: Boolean = false,
    val stable: Boolean = false,
    val ready: Boolean = false,
    val stableProgressMs: Long = 0L,
    val statusMessage: String = "1ゲーム欄を赤枠の中央へ合わせてください",
)

data class FrameMetrics(
    val inkRatio: Float,
    val contentWidthRatio: Float,
    val contentHeightRatio: Float,
    val centerOffsetX: Float,
    val centerOffsetY: Float,
)

object CaptureAutoShutterEvaluator {

    private const val LOG_TAG = "CaptureAutoShutter"
    private const val MIN_INK_RATIO = 0.04f
    private const val MIN_CONTENT_WIDTH_RATIO = 0.30f
    private const val MIN_CONTENT_HEIGHT_RATIO = 0.20f
    private const val MAX_CENTER_OFFSET = 0.28f
    private const val STABLE_DELTA_THRESHOLD = 0.05f
    private const val STABLE_DURATION_MS = 2600L
    private const val ANALYZE_INTERVAL_MS = 250L
    private const val READY_COOLDOWN_MS = 3000L

    fun evaluate(
        metrics: FrameMetrics,
        previous: FrameMetrics?,
        stableSinceMs: Long,
        nowMs: Long,
        lastReadyMs: Long,
    ): Pair<CaptureAutoShutterState, Long> {
        val positioned = metrics.inkRatio >= MIN_INK_RATIO &&
            metrics.contentWidthRatio >= MIN_CONTENT_WIDTH_RATIO &&
            metrics.contentHeightRatio >= MIN_CONTENT_HEIGHT_RATIO &&
            abs(metrics.centerOffsetX) <= MAX_CENTER_OFFSET &&
            abs(metrics.centerOffsetY) <= MAX_CENTER_OFFSET

        val delta = if (previous != null) {
            abs(metrics.inkRatio - previous.inkRatio) +
                abs(metrics.centerOffsetX - previous.centerOffsetX) +
                abs(metrics.centerOffsetY - previous.centerOffsetY)
        } else {
            1f
        }

        val stableNow = positioned && delta < STABLE_DELTA_THRESHOLD
        val newStableSince = if (stableNow) {
            if (stableSinceMs == 0L) nowMs else stableSinceMs
        } else {
            0L
        }
        val stableProgressMs = if (stableNow && newStableSince > 0L) {
            nowMs - newStableSince
        } else {
            0L
        }
        val cooldownActive = lastReadyMs > 0L && (nowMs - lastReadyMs) < READY_COOLDOWN_MS
        val ready = stableProgressMs >= STABLE_DURATION_MS && !cooldownActive

        val statusMessage = when {
            cooldownActive -> "撮影直後です。少し待ってください..."
            !positioned -> "1ゲーム欄を赤枠の中央へ合わせてください"
            !stableNow -> "そのまま静止してください..."
            !ready -> "静止検出中... (${stableProgressMs / 1000 + 1}s)"
            else -> "撮影します"
        }

        if (ready) {
            Log.d(LOG_TAG, "ready ink=${metrics.inkRatio} stableMs=$stableProgressMs")
        }

        return CaptureAutoShutterState(
            positioned = positioned,
            stable = stableNow,
            ready = ready,
            stableProgressMs = stableProgressMs,
            statusMessage = statusMessage,
        ) to newStableSince
    }

    const val ANALYZE_THROTTLE_MS: Long = ANALYZE_INTERVAL_MS
}

class CaptureAutoShutterAnalyzer(
    private val onStateChanged: (CaptureAutoShutterState) -> Unit,
) : ImageAnalysis.Analyzer {

    private var lastMetrics: FrameMetrics? = null
    private var stableSinceMs: Long = 0L
    private var lastAnalyzeMs: Long = 0L
    private var lastReadyMs: Long = 0L

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(image: ImageProxy) {
        try {
            val now = System.currentTimeMillis()
            if (now - lastAnalyzeMs < CaptureAutoShutterEvaluator.ANALYZE_THROTTLE_MS) {
                return
            }
            lastAnalyzeMs = now

            if (image.format != ImageFormat.YUV_420_888) return

            val metrics = extractMetrics(image) ?: return
            val (state, newStableSince) = CaptureAutoShutterEvaluator.evaluate(
                metrics = metrics,
                previous = lastMetrics,
                stableSinceMs = stableSinceMs,
                nowMs = now,
                lastReadyMs = lastReadyMs,
            )
            lastMetrics = metrics
            stableSinceMs = newStableSince
            if (state.ready) {
                lastReadyMs = now
            }
            onStateChanged(state)
        } finally {
            image.close()
        }
    }

    fun reset() {
        lastMetrics = null
        stableSinceMs = 0L
        lastAnalyzeMs = 0L
        lastReadyMs = 0L
    }

    private fun extractMetrics(image: ImageProxy): FrameMetrics? {
        val width = image.width
        val height = image.height
        if (width <= 0 || height <= 0) return null

        val guide = CaptureGuideFrame.toPixelRect(width, height)
        val guideWidth = guide.width()
        val guideHeight = guide.height()
        if (guideWidth <= 0 || guideHeight <= 0) return null

        val yPlane = image.planes[0]
        val yBuffer = yPlane.buffer
        val rowStride = yPlane.rowStride
        val pixelStride = yPlane.pixelStride

        var darkCount = 0
        var sampleCount = 0
        var sumX = 0.0
        var sumY = 0.0
        var minX = guide.right
        var maxX = guide.left
        var minY = guide.bottom
        var maxY = guide.top

        val step = 8
        var y = guide.top
        while (y < guide.bottom) {
            var x = guide.left
            while (x < guide.right) {
                val index = y * rowStride + x * pixelStride
                if (index < yBuffer.limit()) {
                    val luminance = yBuffer.get(index).toInt() and 0xFF
                    sampleCount++
                    if (luminance < 200) {
                        darkCount++
                        sumX += x
                        sumY += y
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                    }
                }
                x += step
            }
            y += step
        }

        if (sampleCount == 0) return null

        val inkRatio = darkCount.toFloat() / sampleCount
        val guideCenterX = (guide.left + guide.right) / 2f
        val guideCenterY = (guide.top + guide.bottom) / 2f

        val centerX = if (darkCount > 0) sumX / darkCount else guideCenterX.toDouble()
        val centerY = if (darkCount > 0) sumY / darkCount else guideCenterY.toDouble()

        val contentWidthRatio = if (darkCount > 0) {
            (maxX - minX + 1).toFloat() / guideWidth
        } else {
            0f
        }
        val contentHeightRatio = if (darkCount > 0) {
            (maxY - minY + 1).toFloat() / guideHeight
        } else {
            0f
        }

        return FrameMetrics(
            inkRatio = inkRatio,
            contentWidthRatio = contentWidthRatio,
            contentHeightRatio = contentHeightRatio,
            centerOffsetX = ((centerX - guideCenterX) / guideWidth).toFloat(),
            centerOffsetY = ((centerY - guideCenterY) / guideHeight).toFloat(),
        )
    }
}
