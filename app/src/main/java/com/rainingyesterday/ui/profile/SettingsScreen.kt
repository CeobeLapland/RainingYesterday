package com.rainingyesterday.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material3.HorizontalDivider
import com.rainingyesterday.domain.model.LocationMode

/**
 * 设置页：瓦片底图源（用户可配置切换海外源）+ 定位模式。
 * 通过注入的 SaveRepository 读写，配置持久化；地图重进/重启后生效。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settings: SettingsViewModel = hiltViewModel(),
) {
    val ui by settings.ui.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        // 顶栏
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Text("‹", style = MaterialTheme.typography.headlineMedium) }
            Text("设置", style = MaterialTheme.typography.titleMedium)
        }
        HorizontalDivider()

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("定位模式", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.clickable { settings.setLocationMode(com.rainingyesterday.domain.model.LocationMode.FOREGROUND) }) {
                RadioButton(selected = ui.locationMode == com.rainingyesterday.domain.model.LocationMode.FOREGROUND, onClick = null)
                Text("前台定位（低耗）", Modifier.align(Alignment.CenterVertically))
            }
            Row(Modifier.clickable { settings.setLocationMode(com.rainingyesterday.domain.model.LocationMode.DISABLE) }) {
                RadioButton(selected = ui.locationMode == com.rainingyesterday.domain.model.LocationMode.DISABLE, onClick = null)
                Text("手点模拟（演示）", Modifier.align(Alignment.CenterVertically))
            }

            HorizontalDivider()
            Text("瓦片底图源（海外源在国内可能加载慢；天地图国内可用但需 key）", style = MaterialTheme.typography.titleSmall)
            com.rainingyesterday.domain.model.TileSource.entries.forEach { source ->
                Row(Modifier.clickable { settings.setTileSource(source) }) {
                    RadioButton(selected = ui.tileSource == source, onClick = null)
                    Text(source.displayName, Modifier.align(Alignment.CenterVertically))
                }
            }
            if (ui.tileSource == com.rainingyesterday.domain.model.TileSource.TIAN_DITU) {
                OutlinedTextField(
                    value = ui.tiandituKey,
                    onValueChange = settings::setTiandituKey,
                    label = { Text("天地图 API key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Text(
                    "在 https://console.tianditu.gov.cn 用浏览器申请 key 填到这里",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (ui.tileSource == com.rainingyesterday.domain.model.TileSource.CUSTOM) {
                OutlinedTextField(
                    value = ui.customTileUrl,
                    onValueChange = settings::setCustomTileUrl,
                    label = { Text("自定义瓦片 URL") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "格式示例：https://a.example.com/tiles/{z}/{x}/{y}.png（{z}{x}{y} 占位必需，可选 {s} 子域）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider()
            Text("迷雾", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.clickable { settings.toggleFog() }) {
                RadioButton(selected = ui.isLayerVisible(com.rainingyesterday.domain.model.MapLayer.FOG), onClick = null)
                Text("显示迷雾", Modifier.align(Alignment.CenterVertically))
            }
        }
    }
}