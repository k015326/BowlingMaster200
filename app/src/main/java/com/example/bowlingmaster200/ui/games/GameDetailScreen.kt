package com.example.bowlingmaster200.ui.games

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bowlingmaster200.domain.model.FrameScore

@Composable
fun GameDetailScreen(
    gameId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameDetailViewModel = viewModel(
        factory = GameDetailViewModel.factory(
            application = LocalContext.current.applicationContext as Application,
            gameId = gameId,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Button(onClick = onBack) {
            Text("Back")
        }

        when {
            uiState.isLoading -> {
                Text(text = "Loading...", modifier = Modifier.padding(top = 16.dp))
            }
            uiState.errorMessage != null -> {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            uiState.detail != null -> {
                val detail = uiState.detail!!
                val metadata = detail.metadata

                Text(
                    text = "Game Detail",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(text = "Date: ${GamesDateFormatter.format(metadata.playedAt)}")
                Text(text = "Location: ${metadata.location ?: "-"}")
                Text(text = "Lane: ${metadata.laneNumber?.toString() ?: "-"}")
                Text(text = "Ball: ${metadata.ballName ?: "-"}")
                Text(
                    text = "Total: ${detail.gameScore.totalScore?.toString() ?: "-"}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(detail.gameScore.frameScores, key = { it.frameIndex }) { frame ->
                        FrameDetailRow(frame = frame)
                    }
                }

                Button(
                    onClick = { viewModel.deleteGame(onDeleted = onBack) },
                    enabled = !uiState.isDeleting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Text(if (uiState.isDeleting) "Deleting..." else "DELETE")
                }
            }
        }
    }
}

@Composable
private fun FrameDetailRow(frame: FrameScore) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "F${frame.frameIndex}")
        Text(
            text = buildString {
                append(formatRoll(frame.firstRoll))
                append(" / ")
                append(formatRoll(frame.secondRoll))
                if (frame.frameIndex == 10) {
                    append(" / ")
                    append(formatRoll(frame.bonusRoll))
                }
            },
        )
        Text(text = "Pins: ${frame.framePins?.toString() ?: "-"}")
        Text(text = "Cum: ${frame.cumulativeScore?.toString() ?: "-"}")
    }
}

private fun formatRoll(value: Int?): String {
    return when (value) {
        null -> "-"
        0 -> "-"
        10 -> "X"
        else -> value.toString()
    }
}
