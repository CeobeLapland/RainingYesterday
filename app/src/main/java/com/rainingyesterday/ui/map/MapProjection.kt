package com.rainingyesterday.ui.map

import com.rainingyesterday.domain.util.Gcj02
import kotlin.math.PI
import kotlin.math.cos

/**
 * 地图投影辅助：把 经纬度 → 屏幕像素，供 Compose Canvas 叠加层自绘。
 * 采用与 OSM 一致的墨卡托近似（EPSG:3857），公式与 osmdroid 兼容，
 * 因此叠加层能与底图瓦片指北对齐（docs/03 §4.2：叠加层 Canvas 自绘、不绑 SDK）。
 *
 * MVP 用"中心点 + zoom"决定比例尺；center 由底图相机回调持续更新。
 */
data class MapProjection(
    val centerLat: Double,
    val centerLng: Double,
    val zoom: Double,
    val pxSizeX: Int,
    val pxSizeY: Int,
    val useGcj: Boolean = false,
) {

    /** 本 zoom 下每像素对应的米数（墨卡托，乘 cos(纬度) 修正） */
    val metersPerPixel: Double
        get() = 156543.03392804097 * cos(centerLat * PI / 180.0) / (2.0.pow(zoom))

    private fun Double.pow(exp: Double): Double = Math.pow(this, exp)

    /** 有效投影中心：GCJ 底图时把中心也转成 GCJ，让叠加层与瓦片同框。 */
    private fun effectiveCenter(): Pair<Double, Double> =
        if (useGcj) Gcj02.wgs84ToGcj02(centerLat, centerLng) else centerLat to centerLng

    /** 把经纬度转成相对屏幕中心（中心点落在屏幕正中）的像素偏移。 */
    fun latLngToPixelOffset(lat: Double, lng: Double): Pair<Double, Double> {
        // 叠加点转 GCJ（若底图为 GCJ）
        val (plat, plng) = if (useGcj) Gcj02.wgs84ToGcj02(lat, lng) else lat to lng
        val (clat, clng) = effectiveCenter()
        val latPerMeter = 1.0 / 111_320.0
        val lngPerMeter = 1.0 / (111_320.0 * cos(clat * PI / 180.0).coerceAtLeast(0.01))
        val dyMeters = (clat - plat) / latPerMeter
        val dxMeters = (plng - clng) / lngPerMeter
        return (dxMeters / metersPerPixel) to (dyMeters / metersPerPixel)
    }

    /** 把像素偏移转回经纬度（用于点击模拟定位）。 */
    fun pixelOffsetToLatLng(offsetX: Double, offsetY: Double): Pair<Double, Double> {
        val (clat, clng) = effectiveCenter()
        val latPerMeter = 1.0 / 111_320.0
        val lngPerMeter = 1.0 / (111_320.0 * cos(clat * PI / 180.0).coerceAtLeast(0.01))
        val dxMeters = offsetX * metersPerPixel
        val dyMeters = offsetY * metersPerPixel
        val rawLat = clat - dyMeters * latPerMeter
        val rawLng = clng + dxMeters * lngPerMeter
        // 底图为 GCJ 时，返回给定位的应该是 WGS84（逆变换近似：GCJ→WGS）
        return if (useGcj) Gcj02.gcj02ToWgs84(rawLat, rawLng) else rawLat to rawLng
    }
}