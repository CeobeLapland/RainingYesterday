package com.rainingyesterday.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

/** 顶层五个 Tab（M0 骨架，对应 docs/01 §3 的 ui 包）。 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    MAP(route = "map", label = "地图", icon = Icons.Filled.LocationOn),
    SPACE(route = "space", label = "我的空间", icon = Icons.Filled.Home),
    DIARY(route = "diary", label = "日记", icon = Icons.Filled.Create),
    COLLECTION(route = "collection", label = "图鉴", icon = Icons.AutoMirrored.Filled.List),
    NPC(route = "npc", label = "对话", icon = Icons.Filled.Person),
}