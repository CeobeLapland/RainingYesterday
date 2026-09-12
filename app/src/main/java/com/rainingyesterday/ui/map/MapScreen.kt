package com.rainingyesterday.ui.map

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.DisposableEffect
import com.rainingyesterday.domain.model.LocationMode
import com.rainingyesterday.ui.theme.Terracotta
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import androidx.compose.runtime.LaunchedEffect

/**
 * M3 地图屏：osmdroid 底图（AndroidView）+ Compose Canvas 叠加层（迷雾六边形/地点标记/图层开关）。
 * 遵循 docs/03 §4.2：底图只作瓦片，叠加层全部 Canvas 自绘，底图可换不影响业务。
 */
@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val projection = remember { mutableStateOf(MapProjection(39.909, 116.397, 17.0, 0, 0)) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 底图：osmdroid
        MapView(projection, viewModel)

        // 叠加层：迷雾 + 地点 + 触发
        val onCanvasSizeChange: (Int, Int) -> Unit = { w, h ->
            projection.value = projection.value.copy(pxSizeX = w, pxSizeY = h, zoom = 17.0)
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            onCanvasSizeChange(size.width.toInt(), size.height.toInt())
            val proj = projection.value
            val cx = size.width / 2f
            val cy = size.height / 2f

            // 足迹：连点成线（docs/02 足迹 / GDD 7.4）
            if (ui.track.size > 1) {
                val path = Path()
                var first = true
                ui.track.forEach { tp ->
                    val (dx, dy) = proj.latLngToPixelOffset(tp.lat, tp.lng)
                    val x = cx + dx.toFloat()
                    val y = cy + dy.toFloat()
                    if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
                }
                drawPath(path = path, color = Color(0xFFA3B88E), style = Stroke(width = 6f))
            }

            // 地点标记：已发现=实心暖色，未发现=半透明轮廓（主动可见）
            ui.places.forEach { place ->
                val (dx, dy) = proj.latLngToPixelOffset(place.lat, place.lng)
                val x = cx + dx.toFloat()
                val y = cy + dy.toFloat()
                val discovered = place.id in ui.discoveredPlaceIds
                if (discovered) {
                    drawCircle(color = Terracotta, radius = 24f, center = Offset(x, y))
                    drawCircle(color = Color.White, radius = 24f, center = Offset(x, y), style = Stroke(width = 4f))
                } else {
                    drawCircle(color = Color(0x66000000), radius = 24f, center = Offset(x, y))
                    drawCircle(color = Color(0xA6FFFFFF), radius = 24f, center = Offset(x, y), style = Stroke(width = 3f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f))))
                }
            }
        }

        // 顶部控制条：迷雾开关 + 定位模式切换
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("迷雾", style = MaterialTheme.typography.labelLarge)
                    Switch(
                        checked = ui.fogVisible,
                        onCheckedChange = { viewModel.toggleFog() },
                    )
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text("定位", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = if (ui.locationMode == LocationMode.DISABLE) "手点模拟" else "前台定位",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // 当前坐标指示
        ui.location?.let { loc ->
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            ) {
                Text(
                    text = "%.5f, %.5f".format(loc.lat, loc.lng),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** osmdroid 地图 View 包装（AndroidView 生命周期管理）。 */
@SuppressLint("MissingPermission")
@Composable
private fun MapView(projection: androidx.compose.runtime.MutableState<MapProjection>, viewModel: MapViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // 初始化 osmdroid 配置（用户代理等）
    LaunchedEffect(Unit) { Configuration.getInstance().userAgentValue = context.packageName }

    DisposableEffect(Unit) {
        onDispose { /* MapView 随 UI 销毁 */ }
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                controller.setZoom(17.0)
                controller.setCenter(GeoPoint(39.909, 116.397))
                setMultiTouchControls(true)
                // 模拟模式：点击地图以设置模拟位置（disable 模式）
            }
        },
        update = { map ->
            val center = map.mapCenter
            projection.value = projection.value.copy(
                centerLat = center.latitude,
                centerLng = center.longitude,
                zoom = map.zoomLevelDouble,
            )
        },
        modifier = Modifier.fillMaxSize(),
    )
}