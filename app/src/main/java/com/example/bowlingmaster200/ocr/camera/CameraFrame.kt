package com.example.bowlingmaster200.ocr.camera

/**
 * カメラフレームの抽象表現。
 * CameraX / OpenCV 等の実装詳細を含まない。
 */
data class CameraFrame(
    val imageBytes: ByteArray,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int = 0,
    val format: ImageFormat = ImageFormat.JPEG,
    val capturedAtMillis: Long = System.currentTimeMillis(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CameraFrame
        return imageBytes.contentEquals(other.imageBytes) &&
            width == other.width &&
            height == other.height &&
            rotationDegrees == other.rotationDegrees &&
            format == other.format &&
            capturedAtMillis == other.capturedAtMillis
    }

    override fun hashCode(): Int {
        var result = imageBytes.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + rotationDegrees
        result = 31 * result + format.hashCode()
        result = 31 * result + capturedAtMillis.hashCode()
        return result
    }
}

enum class ImageFormat {
    JPEG,
    PNG,
    RAW,
}
