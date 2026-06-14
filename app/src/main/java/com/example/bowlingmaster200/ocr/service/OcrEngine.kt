package com.example.bowlingmaster200.ocr.service

/**
 * OCR エンジン差し替えポイント。
 *
 * 本番 OCR 追加時は [OcrEngine] を実装し [OcrServiceFactory] に登録する。
 * 既存コード互換のためインターフェース名は [OcrService] のまま維持。
 */
typealias OcrEngine = OcrService
