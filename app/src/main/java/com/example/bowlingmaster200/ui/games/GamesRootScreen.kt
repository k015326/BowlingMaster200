package com.example.bowlingmaster200.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun GamesRootScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showTabBar = currentRoute == GamesSubRoute.INPUT || currentRoute == GamesSubRoute.HISTORY

    Column(modifier = modifier.fillMaxSize()) {
        if (showTabBar) {
            GamesSubTabBar(
                currentRoute = currentRoute,
                onInputClick = {
                    navController.navigate(GamesSubRoute.INPUT) {
                        popUpTo(GamesSubRoute.INPUT) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onHistoryClick = {
                    navController.navigate(GamesSubRoute.HISTORY) {
                        launchSingleTop = true
                    }
                },
            )
        }

        NavHost(
            navController = navController,
            startDestination = GamesSubRoute.INPUT,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(GamesSubRoute.INPUT) {
                GameInputScreen()
            }
            composable(GamesSubRoute.HISTORY) {
                GamesHistoryScreen(
                    onGameClick = { gameId ->
                        navController.navigate(GamesSubRoute.detail(gameId))
                    },
                )
            }
            composable(GamesSubRoute.DETAIL) { entry ->
                val gameId = entry.arguments?.getString("gameId")?.toLongOrNull()
                if (gameId == null) {
                    Text(
                        text = "Invalid game id",
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    GameDetailScreen(
                        gameId = gameId,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

@Composable
private fun GamesSubTabBar(
    currentRoute: String?,
    onInputClick: () -> Unit,
    onHistoryClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onInputClick,
            enabled = currentRoute != GamesSubRoute.INPUT,
            modifier = Modifier.weight(1f),
        ) {
            Text("Input")
        }
        Button(
            onClick = onHistoryClick,
            enabled = currentRoute != GamesSubRoute.HISTORY,
            modifier = Modifier.weight(1f),
        ) {
            Text("History")
        }
    }
}
