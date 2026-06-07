package com.example.bowlingmaster200.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun FrameInputRow(
    frame: FrameInputUiState,
    onFirstRollChange: (String) -> Unit,
    onSecondRollChange: (String) -> Unit,
    onBonusRollChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val disableSecondRoll = !frame.isTenthFrame && frame.isStrikeFirstRoll
    val showBonusRoll = frame.isTenthFrame

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "F${frame.frameIndex}",
            modifier = Modifier.width(32.dp),
        )

        RollTextField(
            label = "1st",
            value = frame.firstRollText,
            onValueChange = onFirstRollChange,
        )

        RollTextField(
            label = "2nd",
            value = if (disableSecondRoll) "" else frame.secondRollText,
            onValueChange = onSecondRollChange,
            enabled = !disableSecondRoll,
        )

        if (showBonusRoll) {
            RollTextField(
                label = "3rd",
                value = frame.bonusRollText,
                onValueChange = onBonusRollChange,
            )
        } else {
            Text(text = "", modifier = Modifier.width(72.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Pins: ${frame.framePins?.toString() ?: "-"}")
            Text(text = "Cum: ${frame.cumulativeScore?.toString() ?: "-"}")
        }
    }
}

@Composable
private fun RollTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        modifier = Modifier.width(72.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}
