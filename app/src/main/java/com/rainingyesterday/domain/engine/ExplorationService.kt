package com.rainingyesterday.domain.engine

import com.rainingyesterday.domain.model.DiscoveryRecord
import com.rainingyesterday.domain.model.EncyclopediaEntry
import com.rainingyesterday.domain.model.EncyclopediaType
import com.rainingyesterday.domain.model.Place
import com.rainingyesterday.domain.model.TrackPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 探索 / 足迹 / 地点发现（docs/05 M4、GDD 7.2/7.4）。
 * - 地点发现：玩家走进某地点 [radius_m] 内 → 触发[发现]并写图鉴（幂等）。
 * - 足迹：连点成线，仅在与上一个点间隔足够大时记录（去噪）。
 * - 周步数：统计近 7 天的足迹点数量（粗略的"本周步数"）。
 *
 * 纯 Kotlin、无 Android 依赖。输入当前坐标与最新时空，输出应写回的死档操作（由调用方落到 SaveRepository）。
 */
class ExplorationService {

    /** 走进半径检测：返回玩家当前是否在某地点内部。 */
    fun isInside(place: Place, lat: Double, lng: Double): Boolean =
        haversineMeters(lat, lng, place.lat, place.lng) <= place.radiusM

    /**
     * 尝试记录一个足迹点。
     * @return 若应写入返回 TrackPoint，若与上一点太近（去噪）返回 null。
     */
    fun tryAppendTrackPoint(
        lat: Double,
        lng: Double,
        last: TrackPoint?,
        minGapM: Double,
    ): TrackPoint? {
        if (last == null) return TrackPoint(lat, lng)
        val gap = haversineMeters(lat, lng, last.lat, last.lng)
        return if (gap >= minGapM) TrackPoint(lat, lng) else null
    }

    /** 依据已有图鉴（去重），产出应新解锁的地点图鉴条目。 */
    fun discoverPlace(
        place: Place,
        explored: Map<String/*placeId*/, EncyclopediaEntry>,
    ): EncyclopediaEntry? =
        if (place.id in explored) null
        else EncyclopediaEntry(EncyclopediaType.PLACE, place.id)

    /** 周足迹点数（近7天）。时间由调用方统一注入 nowMs，便于测试。 */
    fun weekStepCount(track: List<TrackPoint>, nowMs: Long, sevenDaysMs: Long = SEVEN_DAYS_MS): Int =
        track.count { nowMs - it.timestampMs <= sevenDaysMs }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    companion object {
        private const val SEVEN_DAYS_MS = 7L * 24 * 3600 * 1000
    }
}