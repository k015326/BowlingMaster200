package com.example.bowlingmaster200.ui.games

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

@Composable
fun GameInputScreen(
    modifier: Modifier = Modifier,
    viewModel: GamesViewModel = viewModel(
        factory = GamesViewModel.factory(
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
            text = "Game Input",
            style = MaterialTheme.typography.titleLarge,
        )

        Text(
            text = "Total: ${uiState.totalScore?.toString() ?: "-"}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(uiState.frames, key = { it.frameIndex }) { frame ->
                FrameInputRow(
                    frame = frame,
                    selectedFrameIndex = uiState.selectedFrameIndex,
                    selectedRollIndex = uiState.selectedRollIndex,
                    onFirstRollChange = { viewModel.updateFirstRoll(frame.frameIndex, it) },
                    onSecondRollChange = { viewModel.updateSecondRoll(frame.frameIndex, it) },
                    onBonusRollChange = { viewModel.updateBonusRoll(it) },
                    onCellSelected = { rollIndex ->
                        viewModel.selectCell(frame.frameIndex, rollIndex)
                    },
                )
            }
        }

        uiState.validationError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        uiState.saveMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        ScoreKeypad(
            onKeyPress = viewModel::onKeyInput,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = { viewModel.saveGame() },
            enabled = !uiState.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            Text(if (uiState.isSaving) "Saving..." else "SAVE")
        }
    }
}
