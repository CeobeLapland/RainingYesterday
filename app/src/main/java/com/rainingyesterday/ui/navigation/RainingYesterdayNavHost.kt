package com.rainingyesterday.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rainingyesterday.ui.collection.CollectionScreen
import com.rainingyesterday.ui.diary.DiaryScreen
import com.rainingyesterday.ui.map.MapScreen
import com.rainingyesterday.ui.npc.NpcScreen
import com.rainingyesterday.ui.space.SpaceScreen

/** 应用导航骨架：底部五 Tab + NavHost（M0 占位屏，各系统落地后逐个替换）。 */
@Composable
fun RainingYesterdayNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy
                            ?.any { it.route == destination.route } == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                            )
                        },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.MAP.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopLevelDestination.MAP.route) { MapScreen() }
            composable(TopLevelDestination.SPACE.route) { SpaceScreen() }
            composable(TopLevelDestination.DIARY.route) { DiaryScreen() }
            composable(TopLevelDestination.COLLECTION.route) { CollectionScreen() }
            composable(TopLevelDestination.NPC.route) { NpcScreen() }
        }
    }
}