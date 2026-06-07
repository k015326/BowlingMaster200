package com.example.bowlingmaster200.ui.games

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bowlingmaster200.domain.model.GameMetadata

@Composable
fun GamesHistoryScreen(
    onGameClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GamesHistoryViewModel = viewModel(
        factory = GamesHistoryViewModel.factory(
            LocalContext.current.applicationContext as Application,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Game History",
            style = MaterialTheme.typography.titleLarge,
        )

        when {
            uiState.isLoading -> {
                Text(
                    text = "Loading...",
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            uiState.errorMessage != null -> {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            uiState.games.isEmpty() -> {
                Text(
                    text = "保存済みゲームはありません",
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.games, key = { it.id }) { game ->
                        GameHistoryItem(
                            game = game,
                            onClick = { onGameClick(game.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameHistoryItem(
    game: GameMetadata,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Date: ${GamesDateFormatter.format(game.playedAt)}")
            Text(text = "Total: ${game.totalScore?.toString() ?: "-"}")
            Text(text = "Location: ${game.location ?: "-"}")
            Text(text = "Lane: ${game.laneNumber?.toString() ?: "-"}")
            Text(text = "Ball: ${game.ballName ?: "-"}")
        }
    }
}
