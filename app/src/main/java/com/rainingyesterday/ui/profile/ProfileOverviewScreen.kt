package com.rainingyesterday.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.rainingyesterday.ui.navigation.ProfileRoute
import com.rainingyesterday.ui.navigation.goProfileChild

/** 「我的」概览：入口表格 → 日记/图鉴/设置。 */
@Composable
fun ProfileOverviewScreen(navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("我的", style = MaterialTheme.typography.headlineSmall)
        EntryRow(
            icon = Icons.Filled.Create,
            title = "日记",
            subtitle = "记忆节点 · 回访",
            onClick = { navController.goProfileChild(ProfileRoute.DIARY) },
        )
        EntryRow(
            icon = Icons.Filled.Bookmark,
            title = "图鉴",
            subtitle = "地点 · 自然 · 世界收集",
            onClick = { navController.goProfileChild(ProfileRoute.COLLECTION) },
        )
        EntryRow(
            icon = Icons.Filled.Settings,
            title = "设置",
            subtitle = "定位 · 瓦片 · 迷雾",
            onClick = { navController.goProfileChild(ProfileRoute.SETTINGS) },
        )
    }
}

@Composable
private fun EntryRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.padding(start = 16.dp).weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}