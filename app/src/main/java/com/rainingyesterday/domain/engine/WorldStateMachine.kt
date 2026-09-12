package com.rainingyesterday.domain.engine

import com.rainingyesterday.domain.model.Season
import com.rainingyesterday.domain.model.Weather
import com.rainingyesterday.domain.model.WorldState
import java.time.LocalDate
import java.time.LocalTime

/**
 * 世界状态机（docs/01 §4.1）。持有一份当前 [WorldState]，随时钟推进，可由外部信号写入天气/季节。
 * 纯 Kotlin、可单测：通过注入 [WorldClock] 固定时间。
 *
 * 领域层不接触真实网络 / 传感器，真实信号在 data/signal 层注入（真实优先、模拟兜底），
 * 这里只提供"如何从外部写入状态"的入口。
 */
class WorldStateMachine(
    /** 时间源；缺省用系统时钟 */
    private val clock: WorldClock = SystemWorldClock,
) {

    val isDeepLayer: Boolean
        get() = state.isDeepLayer

    var state: WorldState = snapshotOf(clock)
        private set

    /** 从外部信号更新世界状态。任一参数为 null 表示保持不变。 */
    fun update(
        time: String? = null,
        weather: Weather? = null,
        season: Season? = null,
        date: LocalDate? = null,
        world: WorldState = state,
    ): WorldState {
        val updated = world.copy(
            time = time ?: world.time,
            weather = weather ?: world.weather,
            season = season ?: world.season,
            date = date ?: world.date,
        )
        state = updated
        return updated
    }

    /** 由真实信号写整份世界状态（data/signal 层调用）。 */
    fun setState(newState: WorldState) {
        state = newState
    }

    private fun snapshotOf(clock: WorldClock): WorldState {
        val now = clock.now()
        return WorldState(
            time = "%02d:%02d".format(now.hour, now.minute),
            date = now.date,
            weather = Weather.SUNNY, // 初始兜底，稍后由信号源覆盖
            season = seasonOf(now.date),
        )
    }

    private fun seasonOf(date: LocalDate): Season =
        when (date.monthValue) {
            in 3..5 -> Season.SPRING
            in 6..8 -> Season.SUMMER
            in 9..11 -> Season.AUTUMN
            else -> Season.WINTER
        }
}

/** 时钟抽象，返回日时分，便于测试固定时间。 */
fun interface WorldClock {
    fun now(): LocalDateTimeHolder
}

/** 轻量时间持有者（避免领域层绑 java.time 之外的细节，仅用于状态机快照） */
data class LocalDateTimeHolder(val date: LocalDate, val hour: Int, val minute: Int)

/** 系统时钟实现 */
object SystemWorldClock : WorldClock {
    override fun now(): LocalDateTimeHolder {
        val now = java.time.LocalDateTime.now()
        return LocalDateTimeHolder(now.toLocalDate(), now.hour, now.minute)
    }
}