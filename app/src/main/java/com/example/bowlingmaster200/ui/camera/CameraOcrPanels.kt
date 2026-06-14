package com.example.bowlingmaster200.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OcrStatusBanner(
    uiState: CameraUiState,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.errorMessage != null -> {
            BannerCard(
                text = uiState.errorMessage,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = modifier,
            )
        }
        uiState.isFallbackActive -> {
            BannerCard(
                text = buildString {
                    append("Fallback OCR active")
                    uiState.fallbackReason?.let { append(": $it") }
                },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = modifier,
            )
        }
    }
}

@Composable
fun OcrScoreHeader(
    uiState: CameraUiState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "Total Score",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = uiState.totalScore?.toString() ?: "—",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = uiState.engineId ?: "…",
                style = MaterialTheme.typography.labelMedium,
            )
            uiState.confidence?.let { confidence ->
                Text(
                    text = "Confidence ${formatConfidence(confidence)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "Frames ${uiState.parsedFrameCount}/10",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun OcrFrameScoreGrid(
    frames: List<OcrFrameDisplay>,
    modifier: Modifier = Modifier,
) {
    if (frames.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "Frame Scores",
            style = MaterialTheme.typography.labelLarge,
        )
        frames.chunked(5).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                row.forEach { frame ->
                    OcrFrameCard(
                        frame = frame,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun OcrRecognizedTextPanel(
    rawText: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Recognized Text",
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = rawText ?: "Waiting for capture…",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(8.dp),
                )
                .padding(8.dp),
        )
    }
}

@Composable
fun OcrWarningsPanel(
    warnings: List<String>,
    modifier: Modifier = Modifier,
) {
    if (warnings.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Warnings",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        warnings.forEach { warning ->
            Text(
                text = "• $warning",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun OcrFrameCard(
    frame: OcrFrameDisplay,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .widthIn(min = 56.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp),
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "F${frame.frameIndex}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = buildString {
                    append(frame.roll1)
                    append(' ')
                    append(frame.roll2)
                    frame.roll3?.let { append(' ').append(it) }
                },
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
            )
            Text(
                text = frame.cumulative,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun BannerCard(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = contentColor,
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

private fun formatConfidence(confidence: Float): String {
    return "${(confidence * 100).toInt()}%"
}
