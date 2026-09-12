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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.rainingyesterday.ui.camera.CameraScreen
import com.rainingyesterday.ui.home.HomeScreen
import com.rainingyesterday.ui.map.MapScreen
import com.rainingyesterday.ui.message.MessageScreen
import com.rainingyesterday.ui.profile.ProfileCollection
import com.rainingyesterday.ui.profile.ProfileDiary
import com.rainingyesterday.ui.profile.ProfileOverviewScreen
import com.rainingyesterday.ui.profile.ProfileSettings

/**
 * 应用导航骨架：底部五 Tab（世界/家/摄像/消息/我的）。
 * 「我的」是嵌套图，内含 概览/日记/图鉴/设置 子页。
 */
@Composable
fun RainingYesterdayNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            // 在"我的"内层子页时不显示底部栏（概览才显示）
            if (currentDestination?.route != ProfileRoute.DIARY.route &&
                currentDestination?.route != ProfileRoute.COLLECTION.route &&
                currentDestination?.route != ProfileRoute.SETTINGS.route
            ) {
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
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.WORLD.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopLevelDestination.WORLD.route) {
                MapScreen(
                    onOpenSettings = { navController.goProfileChild(ProfileRoute.SETTINGS) },
                )
            }
            composable(TopLevelDestination.HOME.route) { HomeScreen() }
            composable(TopLevelDestination.CAMERA.route) { CameraScreen() }
            composable(TopLevelDestination.MESSAGE.route) { MessageScreen() }

            // 「我的」嵌套图：概览为入口，内含日记/图鉴/设置子页
            navigation(
                route = TopLevelDestination.PROFILE.route,
                startDestination = ProfileRoute.OVERVIEW.route,
            ) {
                composable(ProfileRoute.OVERVIEW.route) {
                    ProfileOverviewScreen(navController = navController)
                }
                composable(ProfileRoute.DIARY.route) { ProfileDiary() }
                composable(ProfileRoute.COLLECTION.route) { ProfileCollection() }
                composable(ProfileRoute.SETTINGS.route) { ProfileSettings(navController) }
            }
        }
    }
}

/** 供内部子页跳转的便捷函数。 */
fun NavHostController.goProfileChild(route: ProfileRoute) {
    this.navigate(route.route)
}