package com.example.bowlingmaster200.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Games : Screen("games", "Games", Icons.Default.SportsScore)
    data object Stats : Screen("stats", "Stats", Icons.Default.BarChart)

    companion object {
        val bottomNavItems = listOf(Home, Games, Stats)
    }
}
