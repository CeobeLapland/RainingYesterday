package com.rainingyesterday.domain.model

/** 日照（日出/日落）信号，来自真实信号或本地推算。值用 "HH:mm"。 */
data class Daylight(
    val sunrise: String,
    val sunset: String,
) {
    fun minuteOfDay(t: String): Int {
        val p = t.split(":")
        return (p.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (p.getOrNull(1)?.toIntOrNull() ?: 0)
    }
}

/**
 * 领域层玩家的抽象进度快照（docs/02 运行时状态中的玩家侧输入）。
 * 触发引擎据此对 exploration / relationship / has_item / near 条件求值。
 * 真正的玩家存档在 M9 Room 落地，本类作为引擎的纯 Kotlin 输入。
 */
data class PlayerProgress(
    /** 探索度 0~1 */
    val exploration: Double = 0.0,
    /** 熟悉度: npcId -> level */
    val relationships: Map<String, Int> = emptyMap(),
    /** 持有物: itemId -> count */
    val inventory: Map<String, Int> = emptyMap(),
    /** 当前位置（place id，可空 = 不在任何已知地点） */
    val currentPlaceId: String? = null,
    /** 当前真实经纬度（可空） */
    val lat: Double? = null,
    val lng: Double? = null,
    /** 已解锁的历史 key（如 place id），出于可扩展性预留 */
    val unlockedHistory: Set<String> = emptySet(),
)