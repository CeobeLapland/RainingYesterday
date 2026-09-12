package com.rainingyesterday.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 顶层五个 Tab（docs/01 §3 的 ui 包）。
 * 世界(地图) / 家(我的空间) / 摄像(新增) / 消息(对话改名) / 我的(聚合：日记+图鉴+设置)。
 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    WORLD(route = "world", label = "世界", icon = Icons.Filled.Explore),
    HOME(route = "home", label = "家", icon = Icons.Filled.Home),
    CAMERA(route = "camera", label = "摄像", icon = Icons.Filled.PhotoCamera),
    MESSAGE(route = "message", label = "消息", icon = Icons.AutoMirrored.Filled.Chat),
    PROFILE(route = "profile", label = "我的", icon = Icons.Filled.AccountCircle),
}

/** 我的（PROFILE）内的子页面路由。 */
enum class ProfileRoute(val route: String) {
    OVERVIEW("profile/overview"),
    DIARY("profile/diary"),
    COLLECTION("profile/collection"),
    SETTINGS("profile/settings"),
}