package com.example.bowlingmaster200.ui.games

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp

@Composable
fun FrameInputRow(
    frame: FrameInputUiState,
    selectedFrameIndex: Int,
    selectedRollIndex: Int,
    onCellSelected: (rollIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val disableSecondRoll = !frame.isTenthFrame && frame.isStrikeFirstRoll
    val showBonusRoll = frame.isTenthFrame
    val isThisFrameSelected = selectedFrameIndex == frame.frameIndex - 1

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
            value = frame.displayTextForRoll(0),
            onSelect = { onCellSelected(0) },
            isSelected = isThisFrameSelected && selectedRollIndex == 0,
        )

        RollTextField(
            label = "2nd",
            value = if (disableSecondRoll) "" else frame.displayTextForRoll(1),
            enabled = !disableSecondRoll,
            onSelect = { onCellSelected(1) },
            isSelected = isThisFrameSelected && selectedRollIndex == 1,
        )

        if (showBonusRoll) {
            RollTextField(
                label = "3rd",
                value = frame.displayTextForRoll(2),
                onSelect = { onCellSelected(2) },
                isSelected = isThisFrameSelected && selectedRollIndex == 2,
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
    onSelect: () -> Unit,
    isSelected: Boolean = false,
    enabled: Boolean = true,
) {
    val selectionBorder = if (isSelected) {
        Modifier.border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(4.dp),
        )
    } else {
        Modifier
    }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        modifier = Modifier
            .width(72.dp)
            .then(selectionBorder)
            .padding(if (isSelected) 2.dp else 0.dp)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onSelect()
                }
            },
    )
}
