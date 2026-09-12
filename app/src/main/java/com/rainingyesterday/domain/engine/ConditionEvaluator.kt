package com.rainingyesterday.domain.engine

import com.rainingyesterday.domain.model.AllCondition
import com.rainingyesterday.domain.model.AnyCondition
import com.rainingyesterday.domain.model.Condition
import com.rainingyesterday.domain.model.DateCondition
import com.rainingyesterday.domain.model.DayOfWeekCondition
import com.rainingyesterday.domain.model.ExplorationCondition
import com.rainingyesterday.domain.model.HasItemCondition
import com.rainingyesterday.domain.model.NearCondition
import com.rainingyesterday.domain.model.Place
import com.rainingyesterday.domain.model.PlayerProgress
import com.rainingyesterday.domain.model.RelationshipCondition
import com.rainingyesterday.domain.model.SeasonCondition
import com.rainingyesterday.domain.model.TimeRangeCondition
import com.rainingyesterday.domain.model.WeatherCondition
import com.rainingyesterday.domain.model.WorldState
import com.rainingyesterday.domain.model.minuteOfDay

/**
 * 条件的求值器（docs/02 §1）。一次对一组 [Condition] 求值返回是否命中。
 * 纯 Kotlin、无 Android 依赖，可独立单测（docs/01 §4.2 / M2 验收）。
 *
 * 求值所需上下文：
 * - [WorldState]：time / weather / season / date
 * - [PlayerProgress]：exploration / relationships / inventory / 位置
 * - [placeResolver]：把 place id 解析为坐标，用于 `near` 条件（半径距离）。
 */
class ConditionEvaluator(
    private val placeResolver: (String) -> Place? = { null },
) {

    /** 求值单个条件（含 all / any 组合的递归）。unknown Condition 一律视为 false。 */
    fun evaluate(condition: Condition, world: WorldState, player: PlayerProgress): Boolean = when (condition) {
        is AllCondition -> condition.conditions.all { evaluate(it, world, player) }
        is AnyCondition -> condition.conditions.any { evaluate(it, world, player) }
        is TimeRangeCondition -> inRange(world.minuteOfDay(), condition)
        is WeatherCondition -> world.weather == condition.weather
        is SeasonCondition -> world.season == condition.season
        is NearCondition -> near(player, condition)
        is ExplorationCondition -> player.exploration >= condition.min
        is RelationshipCondition -> (player.relationships[condition.npc] ?: 0) >= condition.min
        is HasItemCondition -> (player.inventory[condition.item] ?: 0) >= condition.min
        is DayOfWeekCondition -> condition.days.contains(world.date.dayOfWeek.value)
        is DateCondition -> world.date.monthValue == condition.month && world.date.dayOfMonth == condition.day
    }

    private fun inRange(nowMinute: Int, c: TimeRangeCondition): Boolean {
        val from = resolveMinutes(c.from, nowMinute)
        val to = resolveMinutes(c.to, nowMinute)
        // 处理跨午夜区间，如 from 22:00 to 05:00
        return if (from <= to) nowMinute in from..to else nowMinute >= from || nowMinute <= to
    }

    private fun resolveMinutes(t: String, nowMinute: Int): Int {
        val p = t.split(":")
        if (p.size == 2) return (p[0].toIntOrNull() ?: 0) * 60 + (p[1].toIntOrNull() ?: 0)
        return nowMinute // 支持相对（占位，多数场景用绝对时间）
    }

    private fun near(player: PlayerProgress, c: NearCondition): Boolean {
        val p = player.lat ?: return false
        val l = player.lng ?: return false
        val place = placeResolver(c.place) ?: return false // 地点不存在则为 false
        val distM = haversineMeters(p, l, place.lat, place.lng)
        return distM <= c.radiusM
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}