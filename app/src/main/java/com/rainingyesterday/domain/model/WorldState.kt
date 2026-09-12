package com.rainingyesterday.domain.model

import java.time.LocalDate

/**
 * 当前世界状态（docs/02 §4）。由 [com.rainingyesterday.domain.engine.WorldStateMachine] 推进。
 * 领域层纯 Kotlin 值对象，不可变。time 用 "HH:mm"，date 用 LocalDate。
 */
data class WorldState(
    val time: String,
    val date: LocalDate,
    val weather: Weather,
    val season: Season,
    /** 日照信号（日出/日落，来自真实信号或本地推算），可空 */
    val daylight: Daylight? = null,
    /** 校园动态事件（真实信号或人工维护），缺省空 */
    val campusEvents: List<CampusEvent> = emptyList(),
) {
    /** 世界条件：是否处于深层（翻面）。按 GDD 2.1，深夜/下雨 × 特定→由引擎判定，这里给个默认推断。 */
    val isDeepLayer: Boolean
        get() = isNight || weather == Weather.RAIN
}

/** 当日时间点（分钟级工具类，便于条件求值） */
fun WorldState.minuteOfDay(): Int {
    val parts = time.split(":")
    if (parts.size != 2) return 0
    return (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
}

/** 简单夜间判断：22:00 之后 或 05:00 之前 */
val WorldState.isNight: Boolean
    get() {
        val m = minuteOfDay()
        return m >= 22 * 60 || m < 5 * 60
    }