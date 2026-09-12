package com.rainingyesterday.domain.model

import kotlinx.serialization.Serializable

/**
 * 地图与定位的玩家设置（docs/03 §4.3）。
 * 所有与功耗直接挂钩的参数都可配置，避免硬编码。真实存档在 M9 落 Room。
 */
@Serializable
data class PlayerSettings(
    /** 定位模式：disable(手点模拟) / foreground(前台低耗默认) / background(后台，后补) */
    val locationMode: LocationMode = LocationMode.FOREGROUND,
    /** 定位频率毫秒（前台），绑定功耗 */
    val locationIntervalMs: Long = 15_000,
    /** 最小位移米，避免过于频繁回调 */
    val minDisplacementM: Float = 5f,
    /** 精度优先级：true=高精度，false=省电 */
    val highAccuracy: Boolean = true,
    /** 各图层默认开关（可在运行时读写） */
    val layers: Map<MapLayer, Boolean> = MapLayer.entries
        .associateWith { it.defaultVisible },
    /** 六边形迷雾格边长（米），决定格大小与解锁精度 */
    val hexSideMeters: Double = 20.0,
    /** 走近多少米外解锁迷雾格 */
    val fogRevealRadiusM: Double = 60.0,
) {
    fun isLayerVisible(layer: MapLayer): Boolean = layers[layer] ?: layer.defaultVisible
    fun withLayer(layer: MapLayer, visible: Boolean): PlayerSettings = copy(
        layers = layers + (layer to visible)
    )
}

/** 定位模式（docs/03 §4.3，与 GDD「玩家可配置优先」呼应） */
enum class LocationMode {
    /** 不定位：地图手点模拟位置（演示/开发/无设备） */
    DISABLE,
    /** 前台定位，低耗（默认） */
    FOREGROUND,
    /** 后台轨迹记录（需额外权限与说明，后补） */
    BACKGROUND,
}