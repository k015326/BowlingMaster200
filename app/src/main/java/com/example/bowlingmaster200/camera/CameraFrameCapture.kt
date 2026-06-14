package com.example.bowlingmaster200.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * [ImageProxy] から1フレームを取得し Bitmap に変換する。
 */
object CameraFrameCapture {

    fun fromImageProxy(imageProxy: ImageProxy): CapturedCameraFrame {
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val width = imageProxy.width
        val height = imageProxy.height
        val bitmap = toBitmap(imageProxy)
        imageProxy.close()
        return CapturedCameraFrame(
            bitmap = bitmap,
            width = width,
            height = height,
            rotationDegrees = rotationDegrees,
        )
    }

    private fun toBitmap(imageProxy: ImageProxy): Bitmap {
        return when (imageProxy.format) {
            ImageFormat.JPEG -> decodeJpeg(imageProxy)
            ImageFormat.YUV_420_888 -> yuv420ToBitmap(imageProxy)
            else -> error("Unsupported image format: ${imageProxy.format}")
        }
    }

    private fun decodeJpeg(imageProxy: ImageProxy): Bitmap {
        val buffer = imageProxy.planes[0].buffer
        buffer.rewind()
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: error("Failed to decode JPEG camera frame")
    }

    private fun yuv420ToBitmap(imageProxy: ImageProxy): Bitmap {
        val nv21 = yuv420888ToNv21(imageProxy)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val output = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, output)
        return BitmapFactory.decodeByteArray(output.toByteArray(), 0, output.size())
            ?: error("Failed to decode YUV camera frame")
    }

    private fun yuv420888ToNv21(imageProxy: ImageProxy): ByteArray {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)

        val chromaHeight = imageProxy.height / 2
        val chromaWidth = imageProxy.width / 2
        val rowStride = imageProxy.planes[1].rowStride
        val pixelStride = imageProxy.planes[1].pixelStride

        var offset = ySize
        for (row in 0 until chromaHeight) {
            for (col in 0 until chromaWidth) {
                val uvIndex = row * rowStride + col * pixelStride
                nv21[offset++] = vBuffer.get(uvIndex)
                nv21[offset++] = uBuffer.get(uvIndex)
            }
        }
        return nv21
    }
}
