package com.example.bowlingmaster200.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bowlingmaster200.ui.games.GamesScreen
import com.example.bowlingmaster200.ui.home.HomeScreen
import com.example.bowlingmaster200.ui.stats.StatsScreen

@Composable
fun BowlingNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    Scaffold(
        bottomBar = { BottomNavBar(navController) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Home.route) {
                HomeScreen()
            }
            composable(Screen.Games.route) {
                GamesScreen()
            }
            composable(Screen.Stats.route) {
                StatsScreen()
            }
        }
    }
}
