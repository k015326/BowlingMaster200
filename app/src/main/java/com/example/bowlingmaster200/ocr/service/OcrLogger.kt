package com.example.bowlingmaster200.ocr.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.example.bowlingmaster200.BuildConfig
import com.example.bowlingmaster200.ocr.analyzer.ScoreSheetAnalysisResult
import com.example.bowlingmaster200.ocr.image.OcrInputBitmapPipeline
import com.example.bowlingmaster200.ocr.pipeline.OcrResult
import com.google.mlkit.vision.text.Text

internal object OcrLogger {

    private const val TAG = "BowlingOCR"
    private const val MAX_LOG_CHUNK = 3500

    /** 調査用: 投球結果欄の推定 Y 上限（OCR 画像高さに対する比率）。 */
    private const val BOWLING_RESULT_Y_MAX_RATIO = 0.55f

    /** 調査用: 累計スコア欄の推定 Y 下限（OCR 画像高さに対する比率）。 */
    private const val CUMULATIVE_SCORE_Y_MIN_RATIO = 0.45f

    private const val BBOX_DISTRIBUTION_Y_BINS = 10

    fun d(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG && throwable != null) {
            Log.e(TAG, message, throwable)
        } else {
            Log.w(TAG, message)
        }
    }

    /** 1. OCR実行開始 */
    fun logOcrStart(
        source: String?,
        bitmapWidth: Int?,
        bitmapHeight: Int?,
    ) {
        if (!BuildConfig.DEBUG) return
        Log.d(
            TAG,
            buildString {
                append("[OCR start]")
                source?.let { append(" source=$it") }
                if (bitmapWidth != null && bitmapHeight != null) {
                    append(" bitmap=${bitmapWidth}x${bitmapHeight}")
                }
            },
        )
    }

    /** 2. OCR生テキスト取得 */
    fun logOcrRawText(ocrResult: OcrResult) {
        if (!BuildConfig.DEBUG) return
        val isFallback = ocrResult.debugInfo["fallback"] == "true"
        val primaryRawText = ocrResult.debugInfo["primaryRawText"]
        val primaryLineCount = ocrResult.debugInfo["primaryLineCount"]

        Log.d(
            TAG,
            buildString {
                appendLine("[OCR raw text]")
                appendLine("  engineId=${ocrResult.engineId}")
                appendLine("  lineCount=${ocrResult.lines.size}")
                appendLine("  confidence=${ocrResult.confidence}")
                if (isFallback) {
                    appendLine("  fallback=true")
                    appendLine("  primaryEngine=${ocrResult.debugInfo["primaryEngine"]}")
                    appendLine("  primaryLineCount=${primaryLineCount ?: "0"}")
                    appendLine("  primaryRawText:")
                    appendRawTextLines(primaryRawText.orEmpty())
                    appendLine("  finalRawText (fallback engine):")
                } else {
                    appendLine("  rawText:")
                }
                appendRawTextLines(ocrResult.rawText)
            },
        )
    }

    /** 3. Parse開始 */
    fun logParseStart(engineId: String, rawTextLength: Int, lineCount: Int) {
        if (!BuildConfig.DEBUG) return
        Log.d(
            TAG,
            "[Parse start] engineId=$engineId rawTextLength=$rawTextLength lineCount=$lineCount",
        )
    }

    /** 4. Parse結果 */
    fun logParseResult(analysis: ScoreSheetAnalysisResult) {
        if (!BuildConfig.DEBUG) return
        val parsedFrames = analysis.savedGame.frames.count { it.firstRoll != null }
        val isComplete = parsedFrames == analysis.savedGame.frames.size &&
            analysis.savedGame.frames.all { it.firstRoll != null }
        Log.d(
            TAG,
            buildString {
                appendLine("[Parse result]")
                appendLine("  engineId=${analysis.engineId}")
                appendLine("  parsedFrames=$parsedFrames/10")
                appendLine("  complete=$isComplete")
                appendLine("  confidence=${analysis.confidence}")
                if (analysis.warnings.isNotEmpty()) {
                    appendLine("  warnings=${analysis.warnings.joinToString("; ")}")
                }
            },
        )
    }

    /** 5. Fallback理由 */
    fun logFallbackReason(
        reason: String,
        primaryEngine: String?,
        primaryLineCount: Int?,
        primaryRawText: String?,
        exception: Exception? = null,
    ) {
        if (!BuildConfig.DEBUG) return
        Log.d(
            TAG,
            buildString {
                appendLine("[Fallback] reason=$reason")
                exception?.let { appendLine("  exception=${it.message}") }
                primaryEngine?.let { appendLine("  primaryEngine=$it") }
                primaryLineCount?.let { appendLine("  primaryLineCount=$it") }
                primaryRawText?.let {
                    appendLine("  primaryRawText:")
                    appendRawTextLines(it)
                }
            },
        )
    }

    fun logOcrResult(result: OcrResult) {
        if (!BuildConfig.DEBUG) return
        Log.d(
            TAG,
            buildString {
                appendLine("OCR result")
                appendLine("  engineId=${result.engineId}")
                appendLine("  confidence=${result.confidence}")
                appendLine("  lineCount=${result.lines.size}")
                appendLine("  debugInfo=${result.debugInfo}")
                appendLine("  rawText:")
                appendRawTextLines(result.rawText)
            },
        )
    }

    fun logMlKitVisionText(text: String) {
        Log.d(TAG, "[TEST] logMlKitVisionText entered")
        Log.d(TAG, "[DEBUG CHECK] BuildConfig.DEBUG=${BuildConfig.DEBUG}")
        if (!BuildConfig.DEBUG) return
        logChunked(
            header = "[ML Kit visionText.text] length=${text.length}",
            body = buildString { appendRawTextLines(text) },
        )
    }

    /** ML Kit block/line/element 構造（bbox 付き）。全文確認用。block 単位で1ログ（4KB超は block 内分割）。 */
    fun logMlKitTextStructure(visionText: Text) {
        Log.d(TAG, "[TEST] logMlKitTextStructure entered")
        Log.d(TAG, "[DEBUG CHECK] BuildConfig.DEBUG=${BuildConfig.DEBUG}")
        if (!BuildConfig.DEBUG) return
        val fullText = visionText.text
        val blockCount = visionText.textBlocks.size
        Log.d(
            TAG,
            "[ML Kit text structure] blockCount=$blockCount fullTextLength=${fullText.length}",
        )
        logChunked(
            header = "[ML Kit text structure] fullText",
            body = buildString { appendRawTextLines(fullText) },
        )
        visionText.textBlocks.forEachIndexed { blockIndex, block ->
            val blockBody = buildString {
                appendLine("  block[$blockIndex/$blockCount] text=\"${block.text}\" bbox=${block.boundingBox}")
                block.lines.forEachIndexed { lineIndex, line ->
                    appendLine(
                        "    line[$lineIndex] text=\"${line.text}\" bbox=${line.boundingBox}",
                    )
                    line.elements.forEachIndexed { elementIndex, element ->
                        appendLine(
                            "      elem[$elementIndex] text=\"${element.text}\" " +
                                "bbox=${element.boundingBox}",
                        )
                    }
                }
            }
            logChunked(
                header = "[ML Kit text structure] block[$blockIndex/$blockCount]",
                body = blockBody,
            )
        }
    }

    /**
     * 調査用: OCR 画像内の ML Kit block bbox 分布（投球結果欄 / 累計スコア欄）。
     * 参照 Y 帯はスコアシート一般的レイアウトに基づく調査用推定値（ロジック非使用）。
     */
    fun logMlKitBboxRegionDistribution(
        visionText: Text,
        ocrWidth: Int,
        ocrHeight: Int,
    ) {
        if (!BuildConfig.DEBUG) return
        if (ocrWidth <= 0 || ocrHeight <= 0) {
            Log.d(TAG, "[ML Kit bbox distribution] skipped invalid ocrSize=${ocrWidth}x$ocrHeight")
            return
        }

        val bowlingResultYMaxPx = (ocrHeight * BOWLING_RESULT_Y_MAX_RATIO).toInt()
        val cumulativeScoreYMinPx = (ocrHeight * CUMULATIVE_SCORE_Y_MIN_RATIO).toInt()
        val blocks = visionText.textBlocks

        Log.d(
            TAG,
            buildString {
                appendLine("[ML Kit bbox distribution] ocrSize=${ocrWidth}x$ocrHeight blockCount=${blocks.size}")
                appendLine("  referenceRegions (investigation estimate, normalized Y):")
                appendLine(
                    "    bowling_result: y=0..$bowlingResultYMaxPx " +
                        "(0.0..${"%.3f".format(BOWLING_RESULT_Y_MAX_RATIO)})",
                )
                appendLine(
                    "    cumulative_score: y=$cumulativeScoreYMinPx..$ocrHeight " +
                        "(${"%.3f".format(CUMULATIVE_SCORE_Y_MIN_RATIO)}..1.0)",
                )
                appendLine(
                    "    overlap_band: y=$cumulativeScoreYMinPx..$bowlingResultYMaxPx " +
                        "(both regions)",
                )
            },
        )

        val regionCounts = mutableMapOf(
            "bowling_result" to 0,
            "cumulative_score" to 0,
            "overlap_band" to 0,
            "outside_ocr" to 0,
            "no_bbox" to 0,
        )
        val yBinCounts = IntArray(BBOX_DISTRIBUTION_Y_BINS) { 0 }

        blocks.forEachIndexed { blockIndex, block ->
            val bbox = block.boundingBox
            if (bbox == null) {
                regionCounts["no_bbox"] = regionCounts.getValue("no_bbox") + 1
                Log.d(
                    TAG,
                    "[ML Kit bbox distribution] block[$blockIndex] text=\"${block.text}\" bbox=(null)",
                )
                return@forEachIndexed
            }

            val cx = (bbox.left + bbox.right) / 2
            val cy = (bbox.top + bbox.bottom) / 2
            val width = bbox.width()
            val height = bbox.height()
            val normCx = cx.toFloat() / ocrWidth
            val normCy = cy.toFloat() / ocrHeight
            val region = classifyBboxRegion(cy, bowlingResultYMaxPx, cumulativeScoreYMinPx, ocrHeight)
            regionCounts[region] = regionCounts.getValue(region) + 1

            val binIndex = ((normCy * BBOX_DISTRIBUTION_Y_BINS).toInt())
                .coerceIn(0, BBOX_DISTRIBUTION_Y_BINS - 1)
            yBinCounts[binIndex]++

            Log.d(
                TAG,
                "[ML Kit bbox distribution] block[$blockIndex] " +
                    "text=\"${block.text}\" " +
                    "center=($cx,$cy) size=${width}x$height " +
                    "normCenter=(${"%.3f".format(normCx)},${"%.3f".format(normCy)}) " +
                    "bbox=$bbox region=$region contentHint=${bboxContentHint(block.text)}",
            )
        }

        Log.d(
            TAG,
            buildString {
                appendLine("[ML Kit bbox distribution] regionSummary")
                regionCounts.forEach { (region, count) ->
                    appendLine("  $region=$count")
                }
                appendLine("  yBinHistogram (block center count per horizontal band):")
                for (bin in 0 until BBOX_DISTRIBUTION_Y_BINS) {
                    val yStart = (bin * ocrHeight) / BBOX_DISTRIBUTION_Y_BINS
                    val yEnd = ((bin + 1) * ocrHeight) / BBOX_DISTRIBUTION_Y_BINS
                    val bar = "#".repeat(yBinCounts[bin].coerceAtMost(40))
                    appendLine("    y=$yStart..$yEnd count=${yBinCounts[bin]} $bar")
                }
            },
        )
    }

    fun logMlKitSortedLines(lines: List<Text.Line>) {
        if (!BuildConfig.DEBUG) return
        logIndexedItems(
            header = "[OcrTextNormalizer] ML Kit sorted lines",
            count = lines.size,
        ) { index, total ->
            val line = lines[index]
            "[$index/$total] text=\"${line.text}\" bbox=${line.boundingBox}"
        }
    }

    fun logLineSplitDebug(sourceText: String, sourceBbox: Rect?, parts: List<String>) {
        if (!BuildConfig.DEBUG) return
        d(
            "[OcrTextNormalizer] splitIntoFrameLines " +
                "source=\"$sourceText\" bbox=$sourceBbox " +
                "parts(${parts.size})=${parts.joinToString(" | ") { "\"$it\"" }}",
        )
    }

    fun logNormalizationCandidates(candidateLines: List<String>) {
        if (!BuildConfig.DEBUG) return
        logIndexedItems(
            header = "[OcrTextNormalizer] candidateLines (pre-filter)",
            count = candidateLines.size,
        ) { index, total ->
            "[$index/$total] \"${candidateLines[index]}\""
        }
    }

    fun logOcrRegionDetail(
        debugInfo: OcrInputBitmapPipeline.DebugInfo,
        outputWidth: Int,
        outputHeight: Int,
    ) {
        if (!BuildConfig.DEBUG) return
        val orientedW = debugInfo.orientedWidth
        val orientedH = debugInfo.orientedHeight
        val guide = debugInfo.guideRect
        val ocr = debugInfo.ocrRect
        Log.d(
            TAG,
            buildString {
                appendLine("[OCR region] oriented=${orientedW}x${orientedH} output=${outputWidth}x$outputHeight")
                appendLine("  guideRect=$guide " +
                    "ratio=L${guide.left.toFloat() / orientedW} " +
                    "T${guide.top.toFloat() / orientedH} " +
                    "R${guide.right.toFloat() / orientedW} " +
                    "B${guide.bottom.toFloat() / orientedH}")
                appendLine("  ocrRect=$ocr " +
                    "ratio=L${ocr.left.toFloat() / orientedW} " +
                    "T${ocr.top.toFloat() / orientedH} " +
                    "R${ocr.right.toFloat() / orientedW} " +
                    "B${ocr.bottom.toFloat() / orientedH}")
                appendLine("  trimBottomPx=${debugInfo.trimBottomPx}")
                appendLine("  cropSize=${ocr.width()}x${ocr.height()} " +
                    "afterTrim≈${ocr.width()}x${ocr.height() - debugInfo.trimBottomPx}")
            },
        )
    }

    fun logSnapshotSaved(context: Context, bitmap: Bitmap) {
        logSnapshotBitmapCorrelation(
            context = context,
            phase = "ocr_save",
            bitmap = bitmap,
        )
    }

    /**
     * ML Kit 投入 Bitmap と Debug Snapshot 表示 Bitmap の同一性確認用。
     * BuildConfig ガードなし（実機確認用）。
     */
    fun logSnapshotBitmapCorrelation(
        context: Context,
        phase: String,
        bitmap: Bitmap? = null,
    ) {
        val file = OcrMlKitInputDebugSnapshot.snapshotFile(context)
        val exists = file.exists()
        Log.d(
            TAG,
            buildString {
                append("[Snapshot bitmap correlation] phase=$phase")
                if (bitmap != null) {
                    append(" bitmapWidth=${bitmap.width}")
                    append(" bitmapHeight=${bitmap.height}")
                    append(" bitmapHashCode=${bitmap.hashCode()}")
                } else {
                    append(" bitmap=(null)")
                }
                append(" snapshotPath=${file.absolutePath}")
                append(" snapshotExists=$exists")
                if (exists) {
                    append(" snapshotBytes=${file.length()}")
                    append(" snapshotLastModified=${file.lastModified()}")
                }
            },
        )
    }

    fun logSnapshotCorrelation(
        context: Context,
        phase: String,
        mlKitTextLength: Int? = null,
        blockCount: Int? = null,
        candidateLineCount: Int? = null,
        acceptedFrameCount: Int? = null,
    ) {
        if (!BuildConfig.DEBUG) return
        val file = OcrMlKitInputDebugSnapshot.snapshotFile(context)
        Log.d(
            TAG,
            buildString {
                append("[OCR snapshot correlation] phase=$phase")
                append(" snapshot=${file.absolutePath}")
                append(" exists=${file.exists()}")
                if (file.exists()) {
                    append(" bytes=${file.length()} modified=${file.lastModified()}")
                }
                mlKitTextLength?.let { append(" mlKitTextLength=$it") }
                blockCount?.let { append(" blockCount=$it") }
                candidateLineCount?.let { append(" candidateLineCount=$it") }
                acceptedFrameCount?.let { append(" acceptedFrames=$it") }
            },
        )
    }

    fun logFilterPipelineSummary(
        mlKitTextLength: Int,
        blockCount: Int,
        sortedLineCount: Int,
        droppedLineCount: Int,
        candidateLineCount: Int,
        acceptedLineCount: Int,
        rejectedLineCount: Int,
        filteredRawTextLength: Int,
    ) {
        if (!BuildConfig.DEBUG) return
        Log.d(
            TAG,
            buildString {
                appendLine("[OcrTextNormalizer] filter pipeline summary")
                appendLine("  mlKitTextLength=$mlKitTextLength blockCount=$blockCount")
                appendLine("  sortedLineCount=$sortedLineCount droppedLineCount=$droppedLineCount")
                appendLine("  candidateLineCount=$candidateLineCount")
                appendLine("  acceptedLineCount=$acceptedLineCount rejectedLineCount=$rejectedLineCount")
                appendLine("  filteredRawTextLength=$filteredRawTextLength")
                if (filteredRawTextLength == 0 && mlKitTextLength > 0) {
                    appendLine("  note=ML Kit text present but all lines rejected by filter")
                }
            },
        )
    }

    fun logFilterLinesBoundary(
        phase: String,
        lines: List<String>,
    ) {
        if (!BuildConfig.DEBUG) return
        logIndexedItems(
            header = "[OcrAnalyzerInputFilter] filterLines $phase lineCount=${lines.size}",
            count = lines.size,
        ) { index, total ->
            "[$index/$total] \"${lines[index]}\""
        }
    }

    fun logFilterLinesAfter(result: OcrAnalyzerInputFilter.FilterResult) {
        if (!BuildConfig.DEBUG) return
        Log.d(
            TAG,
            buildString {
                appendLine("[OcrAnalyzerInputFilter] filterLines after")
                appendLine("  acceptedLineCount=${result.acceptedLines.size}")
                appendLine("  rejectedLineCount=${result.rejectedLines.size}")
                appendLine("  acceptedLines:")
                if (result.acceptedLines.isEmpty()) {
                    appendLine("    | (empty)")
                } else {
                    result.acceptedLines.forEachIndexed { index, line ->
                        appendLine("    [$index] $line")
                    }
                }
                appendLine("  rejectedLines:")
                if (result.rejectedLines.isEmpty()) {
                    appendLine("    | (empty)")
                } else {
                    result.rejectedLines.forEachIndexed { index, line ->
                        appendLine("    [$index] $line")
                    }
                }
                appendLine("  outputRawText:")
                appendRawTextLines(result.rawText)
            },
        )
    }

    fun logAnalyzerInputFilter(
        inputLines: List<String>,
        result: OcrAnalyzerInputFilter.FilterResult,
        verdicts: List<OcrAnalyzerInputFilter.FilterLineVerdict>,
    ) {
        if (!BuildConfig.DEBUG) return
        Log.d(
            TAG,
            "[OcrAnalyzerInputFilter] filterLines " +
                "inputLineCount=${inputLines.size} " +
                "accepted=${result.acceptedLines.size} " +
                "rejected=${result.rejectedLines.size} " +
                "outputRawTextLength=${result.rawText.length}",
        )
        logIndexedItems(
            header = "[OcrAnalyzerInputFilter] inputLines",
            count = inputLines.size,
        ) { index, total ->
            "[$index/$total] \"${inputLines[index]}\""
        }
        logIndexedItems(
            header = "[OcrAnalyzerInputFilter] acceptedLines",
            count = result.acceptedLines.size,
        ) { index, total ->
            "[$index/$total] \"${result.acceptedLines[index]}\""
        }
        logIndexedItems(
            header = "[OcrAnalyzerInputFilter] rejectedLines",
            count = result.rejectedLines.size,
        ) { index, total ->
            "[$index/$total] \"${result.rejectedLines[index]}\""
        }
        logChunked(
            header = "[OcrAnalyzerInputFilter] outputRawText",
            body = buildString { appendRawTextLines(result.rawText) },
        )
        logFilterLineVerdicts(verdicts)
        if (result.acceptedLines.isEmpty()) {
            logAcceptedLinesZeroSummary(verdicts, result.rejectedLines.size)
        }
    }

    fun logFilterLineVerdicts(verdicts: List<OcrAnalyzerInputFilter.FilterLineVerdict>) {
        if (!BuildConfig.DEBUG) return
        Log.d(TAG, "[OcrAnalyzerInputFilter] lineVerdicts count=${verdicts.size}")
        logIndexedItems(
            header = "[OcrAnalyzerInputFilter] lineVerdicts",
            count = verdicts.size,
        ) { index, total ->
            val verdict = verdicts[index]
            "[$index/$total] ${verdict.verdict}: \"${verdict.line}\" reason=${verdict.reason}"
        }
    }

    fun logAcceptedLinesZeroSummary(
        verdicts: List<OcrAnalyzerInputFilter.FilterLineVerdict>,
        rejectedLineCount: Int,
    ) {
        if (!BuildConfig.DEBUG) return
        val rejectReasonCounts = verdicts
            .filter { it.verdict == "REJECT" }
            .groupingBy { it.reason }
            .eachCount()
        val skipReasonCounts = verdicts
            .filter { it.verdict == "SKIP" }
            .groupingBy { it.reason }
            .eachCount()
        Log.d(
            TAG,
            buildString {
                appendLine("[OcrAnalyzerInputFilter] acceptedLines=0 summary")
                appendLine("  totalVerdicts=${verdicts.size}")
                appendLine("  rejectedLineCount=$rejectedLineCount")
                appendLine("  acceptCount=0")
                appendLine("  rejectCount=${verdicts.count { it.verdict == "REJECT" }}")
                appendLine("  skipCount=${verdicts.count { it.verdict == "SKIP" }}")
                if (rejectReasonCounts.isEmpty()) {
                    appendLine("  rejectReasons: (none)")
                } else {
                    appendLine("  rejectReasons:")
                    rejectReasonCounts.entries
                        .sortedByDescending { it.value }
                        .forEach { (reason, count) ->
                            appendLine("    $reason=$count")
                        }
                }
                if (skipReasonCounts.isNotEmpty()) {
                    appendLine("  skipReasons:")
                    skipReasonCounts.entries
                        .sortedByDescending { it.value }
                        .forEach { (reason, count) ->
                            appendLine("    $reason=$count")
                        }
                }
                val dominantReason = rejectReasonCounts.maxByOrNull { it.value }?.key
                if (dominantReason != null) {
                    appendLine("  likelyCause=all_candidates_rejected dominantReason=$dominantReason")
                } else if (verdicts.isEmpty()) {
                    appendLine("  likelyCause=no_input_lines")
                } else {
                    appendLine("  likelyCause=all_candidates_skipped_or_empty")
                }
            },
        )
    }

    fun logEmptyTextPrimarySnapshot(
        primaryRawText: String,
        primaryLineCount: Int,
        acceptedLineCount: Int,
        rejectedLineCount: Int,
    ) {
        if (!BuildConfig.DEBUG) return
        Log.d(
            TAG,
            buildString {
                appendLine("[empty_text] primary snapshot before fallback")
                appendLine("  primaryLineCount=$primaryLineCount")
                appendLine("  acceptedLineCount=$acceptedLineCount")
                appendLine("  rejectedLineCount=$rejectedLineCount")
                appendLine("  primaryRawText:")
                appendRawTextLines(primaryRawText)
            },
        )
    }

    private fun StringBuilder.appendRawTextLines(text: String) {
        if (text.isBlank()) {
            appendLine("    | (empty)")
        } else {
            text.lines().forEach { line ->
                appendLine("    | $line")
            }
        }
    }

    private fun logChunked(header: String, body: String) {
        if (body.length <= MAX_LOG_CHUNK) {
            Log.d(TAG, "$header\n$body")
            return
        }
        var part = 1
        var offset = 0
        while (offset < body.length) {
            val end = (offset + MAX_LOG_CHUNK).coerceAtMost(body.length)
            Log.d(TAG, "$header (part $part)\n${body.substring(offset, end)}")
            offset = end
            part++
        }
    }

    private fun logIndexedItems(
        header: String,
        count: Int,
        formatter: (index: Int, total: Int) -> String,
    ) {
        if (count == 0) {
            Log.d(TAG, "$header (empty)")
            return
        }
        Log.d(TAG, "$header count=$count")
        for (index in 0 until count) {
            Log.d(TAG, formatter(index, count))
        }
    }

    private fun classifyBboxRegion(
        centerY: Int,
        bowlingResultYMaxPx: Int,
        cumulativeScoreYMinPx: Int,
        ocrHeight: Int,
    ): String {
        if (centerY < 0 || centerY > ocrHeight) return "outside_ocr"
        return when {
            centerY <= bowlingResultYMaxPx && centerY >= cumulativeScoreYMinPx -> "overlap_band"
            centerY <= bowlingResultYMaxPx -> "bowling_result"
            centerY >= cumulativeScoreYMinPx -> "cumulative_score"
            else -> "outside_ocr"
        }
    }

    /** 調査用テキスト分類（フィルタロジックとは無関係）。 */
    private fun bboxContentHint(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return "empty"
        if (trimmed.matches(Regex("""^\d{1,3}$"""))) return "numeric_only"
        if (trimmed.contains(Regex("""[Ff]""")) || trimmed.contains(':')) return "frame_prefix"
        if (trimmed.contains(Regex("""[/Xx\-,]"""))) return "roll_symbols"
        return "other"
    }
}
