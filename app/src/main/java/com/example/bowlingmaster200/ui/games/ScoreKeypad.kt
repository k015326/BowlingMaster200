package com.example.bowlingmaster200.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ScoreKeypad(
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KeypadRow(keys = listOf("1", "2", "3", "X"), onKeyPress = onKeyPress)
        KeypadRow(keys = listOf("4", "5", "6", "/"), onKeyPress = onKeyPress)
        KeypadRow(keys = listOf("7", "8", "9", "G"), onKeyPress = onKeyPress)
        KeypadRow(keys = listOf("-", "F", "DEL"), onKeyPress = onKeyPress)
    }
}

@Composable
private fun KeypadRow(
    keys: List<String>,
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        keys.forEach { key ->
            Button(
                onClick = { onKeyPress(key) },
                modifier = Modifier.weight(1f),
            ) {
                Text(text = key)
            }
        }
    }
}
