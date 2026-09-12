package com.rainingyesterday.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Layers
import com.rainingyesterday.domain.model.HexGrid
import com.rainingyesterday.domain.model.LocationMode
import com.rainingyesterday.domain.model.TileSource
import com.rainingyesterday.ui.theme.Terracotta
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import android.view.MotionEvent

/**
 * M3 地图屏：osmdroid 底图（AndroidView）+ Compose Canvas 叠加层。
 * 叠加层（迷雾/足迹/地点/玩家点）全部 Canvas 自绘，不绑地图 SDK（docs/03 §4.2）。
 * 迷雾=矢量多边形挖洞：整屏铺雾色，BlendMode.Clear 把已解锁六边形挖成透明，露出底图与标记。
 */
@Composable
fun MapScreen(
    onOpenSettings: () -> Unit = {},
    viewModel: MapViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val useGcj = ui.tileSource == TileSource.AMAP
    val projection = remember { mutableStateOf(MapProjection(INIT_LAT, INIT_LNG, 17.0, 0, 0, useGcj)) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 底图：osmdroid（跟随玩家，保证探索区可见）
        OsmdroidMap(projection, ui.tileSource, ui.tiandituKey, ui.customTileUrl, ui.center) { lat, lng -> viewModel.onMapTap(lat, lng) }

        // 叠加层（Compose Canvas 自绘）
        Canvas(modifier = Modifier.fillMaxSize()) {
            projection.value = projection.value.copy(
                pxSizeX = size.width.toInt(), pxSizeY = size.height.toInt(), useGcj = useGcj,
            )
            val proj = projection.value
            val cx = size.width / 2f
            val cy = size.height / 2f

            // ===== 迷雾：整屏铺暗色，再把已解锁六边形 carve 成透明露出底图 =====
            // 格子坐标来自 HexGrid（与存档同一种子，世界原点固定），钉死在地球坐标，绝不随玩家移动。
            // exploredKeys 里"当前格+六邻格"由 FogService 一次给出 → carv
            // 成连贯探索区；玩家真实行走才解锁新格，越走越开。
            if (ui.fogVisible) {
                drawIntoCanvas { canvas ->
                    val ac = canvas.nativeCanvas
                    val fogPaint = android.graphics.Paint().apply { color = FOG_COLOR_INT }
                    val clear = android.graphics.Paint().apply {
                        isAntiAlias = true
                        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                    }
                    ac.saveLayer(null, null, android.graphics.Canvas.ALL_SAVE_FLAG)
                    ac.drawRect(0f, 0f, size.width, size.height, fogPaint)
                    val sidePx = (ui.fogHexSideMeters / proj.metersPerPixel).toFloat()
                    if (ui.exploredKeys.isNotEmpty() && sidePx > 2f) {
                        val carveR = sidePx * 1.9f
                        for (key in ui.exploredKeys) {
                            val axial = HexGrid.parseKey(key) ?: continue
                            val (hlat, hlng) = HexGrid.axialToLatLng(
                                axial, ui.fogOriginLat, ui.fogOriginLng, ui.fogHexSideMeters,
                            )
                            val (hdx, hdy) = proj.latLngToPixelOffset(hlat, hlng)
                            ac.drawPath(
                                hexPath(cx + hdx.toFloat(), cy + hdy.toFloat(), carveR).asAndroidPath(),
                                clear,
                            )
                        }
                    }
                    ac.restore()
                }
            }

            // ===== 足迹：连点成线 =====
            if (ui.track.size > 1) {
                val path = Path()
                var first = true
                ui.track.forEach { tp ->
                    val (dx, dy) = proj.latLngToPixelOffset(tp.lat, tp.lng)
                    val x = cx + dx.toFloat()
                    val y = cy + dy.toFloat()
                    if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
                }
                drawPath(path, Color(0xFFA3B88E), style = Stroke(width = 6f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(40f, 24f))))
            }

            // ===== 地点标记：已发现=实心暖色，未发现=虚线轮廓 =====
            ui.places.forEach { place ->
                val (dx, dy) = proj.latLngToPixelOffset(place.lat, place.lng)
                val x = cx + dx.toFloat()
                val y = cy + dy.toFloat()
                val discovered = place.id in ui.discoveredPlaceIds
                if (discovered) {
                    drawCircle(Terracotta, radius = 22f, center = Offset(x, y))
                    drawCircle(Color.White, radius = 22f, center = Offset(x, y), style = Stroke(width = 4f))
                } else {
                    drawCircle(Color(0x66000000), radius = 22f, center = Offset(x, y))
                    drawCircle(Color(0xA6FFFFFF), radius = 22f, center = Offset(x, y),
                        style = Stroke(width = 3f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f))))
                }
            }

            // ===== 玩家位置点 =====
            ui.location?.let { loc ->
                val (dx, dy) = proj.latLngToPixelOffset(loc.lat, loc.lng)
                val x = cx + dx.toFloat()
                val y = cy + dy.toFloat()
                drawCircle(Color(0xFFF0A868), radius = 14f, center = Offset(x, y))
                drawCircle(Color.White, radius = 14f, center = Offset(x, y), style = Stroke(width = 4f))
            }
        }

        // ===== 顶部工具栏：顶格占上方一小部分，含 定位/迷雾 与 设置按钮 =====
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("昨夜有雨", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { viewModel.cycleLocationMode() }) {
                        Text(
                            text = when (ui.locationMode) {
                                LocationMode.DISABLE -> "手点"
                                LocationMode.FOREGROUND -> "定位"
                                LocationMode.BACKGROUND -> "后台"
                            },
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    IconToggleButton(checked = ui.fogVisible, onCheckedChange = { viewModel.toggleFog() }) {
                        Icon(
                            imageVector = if (ui.fogVisible) Icons.Filled.Layers else Icons.Outlined.Layers,
                            contentDescription = "迷雾",
                            tint = if (ui.fogVisible) Terracotta else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
                    }
                }
            }
        }
    }
}

/** 六边形顶点路径（cx,cy 为中心，radius 为六边形水平半径）。 */
private fun hexPath(cx: Float, cy: Float, radius: Float): Path = Path().apply {
    for (i in 0 until 6) {
        val angleDeg = 60.0 * i - 30.0
        val px = cx + radius * Math.cos(Math.toRadians(angleDeg)).toFloat()
        val py = cy + radius * Math.sin(Math.toRadians(angleDeg)).toFloat()
        if (i == 0) moveTo(px, py) else lineTo(px, py)
    }
    close()
}

// 迷雾层常量：深雾蓝遮住未解锁地形
private const val FOG_COLOR = 0xCC222A33L
private val FOG_COLOR_INT: Int = FOG_COLOR.toInt()

/** osmdroid 瓦片下载 UA：缺合法浏览器 UA 会被 OSM/高德拒绝 → 灰格子。 */
private const val BROWSER_UA =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

/** osmdroid 底图包装：拖拽/缩放时回写投影，点击回传经纬度（disable 模式设模拟位置）。 */
@Composable
private fun OsmdroidMap(
    projection: androidx.compose.runtime.MutableState<MapProjection>,
    tileSource: TileSource,
    tiandituKey: String,
    customTileUrl: String,
    follow: CenterIntent?,
    onTap: (Double, Double) -> Unit,
) {
    val currentOnTap by rememberUpdatedState(onTap)

    val resolvedSource = remember(tileSource, tiandituKey, customTileUrl) {
        tileSource.resolve(tiandituKey, customTileUrl)
    }

    AndroidView(
        factory = { ctx ->
            // osmdroid 全局配置必须在 MapView 创建【之前】初始化：
            // 缺浏览器 UA（旧代码在 LaunchedEffect 里、晚于 MapView 触发下载）会让 OSM/高德
            // 直接拒绝瓦片请求 → 灰格子 → 也是"迷雾满屏黑"的根因。
            Configuration.getInstance().apply {
                userAgentValue = BROWSER_UA
                osmdroidBasePath = ctx.cacheDir
                osmdroidTileCache = ctx.cacheDir
            }
            MapView(ctx).apply {
                setTileSource(resolvedSource)
                // 初始视野：海淀（一处校园坐标），示例地点围绕它
                controller.setZoom(17.0)
                controller.setCenter(GeoPoint(INIT_LAT, INIT_LNG))
                setMultiTouchControls(true)
                setUseDataConnection(true)
                // 单点：回传给 ViewModel（disable=设置模拟位置）
                overlays.add(object : Overlay() {
                    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                        val g = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt())
                        currentOnTap(g.latitude, g.longitude)
                        return true
                    }
                })
            }
        },
        update = { map ->
            // 切换瓦片源（设置页改了后生效）
            map.setTileSource(resolvedSource)
            // 跟随玩家：玩家位置距离当前视野中心足够远时，平移相机使其可见
            val followPoint = follow?.let { GeoPoint(it.lat, it.lng) }
            if (followPoint != null) {
                val c = map.mapCenter
                val dist = floatArrayOf(1f)
                android.location.Location.distanceBetween(
                    c.latitude, c.longitude, followPoint.latitude, followPoint.longitude, dist,
                )
                if (dist[0] > FOLLOW_RECENTER_DIST_M) {
                    map.controller.setCenter(followPoint)
                }
            }
            val center = map.mapCenter
            projection.value = projection.value.copy(
                centerLat = center.latitude,
                centerLng = center.longitude,
                zoom = map.zoomLevelDouble,
            )
            // 监听地图点击（模拟定位）
            map.setMapListener(object : MapListener {
                override fun onScroll(event: ScrollEvent?): Boolean {
                    val c = map.mapCenter
                    projection.value = projection.value.copy(centerLat = c.latitude, centerLng = c.longitude, zoom = map.zoomLevelDouble)
                    return false
                }
                override fun onZoom(event: ZoomEvent?): Boolean {
                    val c = map.mapCenter
                    projection.value = projection.value.copy(centerLat = c.latitude, centerLng = c.longitude, zoom = map.zoomLevelDouble)
                    return false
                }
            })
        },
        modifier = Modifier.fillMaxSize(),
    )
}

private const val INIT_LAT = 39.992
private const val INIT_LNG = 116.312
/** 玩家偏离视野中心超过该距离（米）时相机跟随平移 */
private const val FOLLOW_RECENTER_DIST_M = 200f

/** TileSource → osmdroid 瓦片源。天地图/自定义需用户提供的 URL 参数。 */
private fun TileSource.resolve(
    tiandituKey: String,
    customUrl: String,
): org.osmdroid.tileprovider.tilesource.ITileSource = when (this) {
    TileSource.AMAP -> XYTileSource(
        "amap", 3, 19, 256, ".png",
        arrayOf("1", "2", "3", "4"),
        "https://webrd0{s}.is.autonavi.com/appmaptile?style=7&x={x}&y={y}&z={z}&lang=zh_cn&size=1&scale=1",
    )
    TileSource.OSM_MAPNIK -> TileSourceFactory.MAPNIK
    TileSource.USGS_SAT -> TileSourceFactory.USGS_SAT
    TileSource.USGS_TOPO -> TileSourceFactory.USGS_TOPO
    TileSource.CUSTOM -> if (customUrl.isNotBlank()) {
        XYTileSource(
            "custom", 0, 19, 256, ".png",
            arrayOf("a", "b", "c", "d"),
            customUrl,
        )
    } else TileSourceFactory.MAPNIK
    TileSource.TIAN_DITU -> if (tiandituKey.isNotBlank()) {
        // 天地图 WMTS（vec=矢量道路），行/列=x/y，用 XYZ 映射到 z/x/y
        XYTileSource(
            "tianditu_vec", 0, 18, 256, ".png",
            arrayOf("t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7"),
            "https://t{s}.tianditu.gov.cn/vec_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0" +
                "&LAYER=vec&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles" +
                "&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=$tiandituKey",
        )
    } else TileSourceFactory.MAPNIK
}