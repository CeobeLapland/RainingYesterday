package com.rainingyesterday.domain.util

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 坐标系转换（docs/03 §4.1 高德需求）。
 *
 * 国内地图（高德/腾讯）用 GCJ-02 加密坐标系，与 GPS/WGS84 有偏移。
 * 底图若用高德瓦片，叠加层（定位/迷雾/地点/足迹）的 WGS84 坐标必须先转 GCJ-02 才能对齐底图。
 * 天地图 / OSM 用 WGS84（CGCS2000 差异 <1m），无需转换。
 */
object Gcj02 {
    private const val PI = 3.1415926535897932384626
    private const val A = 6378245.0
    private const val EE = 0.00669342162296594323

    private fun outOfChina(lat: Double, lng: Double): Boolean =
        lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271

    private fun transformLat(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * sin(y / 12.0 * PI) + 320.0 * sin(y * PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    private fun transformLng(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }

    /** WGS84 → GCJ02。返回 pair(lat, lng)。 */
    fun wgs84ToGcj02(lat: Double, lng: Double): Pair<Double, Double> {
        if (outOfChina(lat, lng)) return lat to lng
        val dLat = transformLat(lng - 105.0, lat - 35.0)
        val dLng = transformLng(lng - 105.0, lat - 35.0)
        val radLat = lat / 180.0 * PI
        var magic = sin(radLat)
        magic = 1 - EE * magic * magic
        val sqrtMagic = sqrt(magic)
        val lat2 = lat + (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI)
        val lng2 = lng + (dLng * 180.0) / (A / sqrtMagic * cos(radLat) * PI)
        return lat2 to lng2
    }

    /** GCJ02 → WGS84（迭代两次反解近似）。返回 pair(lat, lng)。 */
    fun gcj02ToWgs84(lat: Double, lng: Double): Pair<Double, Double> {
        if (outOfChina(lat, lng)) return lat to lng
        // 两次逼近
        var latW = lat
        var lngW = lng
        repeat(2) {
            val (glat, glng) = wgs84ToGcj02(latW, lngW)
            latW = lat - (glat - latW)
            lngW = lng - (glng - lngW)
        }
        return latW to lngW
    }
}