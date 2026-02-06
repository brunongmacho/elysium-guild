package com.elysium.guild.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.elysium.guild.ui.screens.*
import com.elysium.guild.utils.PreferenceManager

@Composable
fun ElysiumNavigation(
    preferenceManager: PreferenceManager
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.BossTimers.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.BossTimers.route) {
                BossTimersScreen(navController, preferenceManager = preferenceManager)
            }
            composable(Screen.Events.route) {
                EventsScreen(navController, preferenceManager = preferenceManager)
            }
            composable(Screen.Leaderboard.route) {
                LeaderboardScreen(navController)
            }
            composable(Screen.Settings.route) {
                ProfileScreen(
                    navController = navController,
                    preferenceManager = preferenceManager
                )
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(
        route = Screen.BossTimers.route,
        label = "Bosses",
        icon = Icons.Filled.Timer
    ),
    BottomNavItem(
        route = Screen.Events.route,
        label = "Events",
        icon = Icons.Filled.Event
    ),
    BottomNavItem(
        route = Screen.Leaderboard.route,
        label = "Ranks",
        icon = Icons.Filled.Leaderboard
    ),
    BottomNavItem(
        route = Screen.Settings.route,
        label = "Settings",
        icon = Icons.Filled.Settings
    )
)

sealed class Screen(val route: String) {
    object BossTimers : Screen("boss_timers")
    object Events : Screen("events")
    object Leaderboard : Screen("leaderboard")
    object Settings : Screen("settings")
}
