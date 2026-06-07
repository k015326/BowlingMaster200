package com.example.bowlingmaster200

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.bowlingmaster200.navigation.BowlingNavGraph
import com.example.bowlingmaster200.ui.theme.BowlingMasterTheme

@Composable
fun BowlingMasterApp() {
    BowlingMasterTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            BowlingNavGraph()
        }
    }
}
