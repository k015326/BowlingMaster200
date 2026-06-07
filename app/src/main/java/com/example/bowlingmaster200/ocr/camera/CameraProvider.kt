package com.example.bowlingmaster200.ocr.camera

/**
 * カメラ取得の抽象インターフェース。
 * 将来 CameraX 実装を差し替え可能にする。
 */
interface CameraProvider {

    val providerId: String

    suspend fun open(): CameraOpenResult

    suspend fun captureFrame(): Result<CameraFrame>

    suspend fun close()
}

enum class CameraOpenResult {
    READY,
    UNAVAILABLE,
    PERMISSION_DENIED,
}

/**
 * 未接続スタブ。UI 接続テスト用。
 */
object UnavailableCameraProvider : CameraProvider {

    override val providerId: String = "unavailable"

    override suspend fun open(): CameraOpenResult = CameraOpenResult.UNAVAILABLE

    override suspend fun captureFrame(): Result<CameraFrame> {
        return Result.failure(IllegalStateException("Camera not available"))
    }

    override suspend fun close() = Unit
}
