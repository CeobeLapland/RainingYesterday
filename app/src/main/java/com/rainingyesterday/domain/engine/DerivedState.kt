package com.rainingyesterday.domain.engine

import com.rainingyesterday.domain.model.Effect
import com.rainingyesterday.domain.model.RandomEvent
import com.rainingyesterday.domain.model.Ritual
import com.rainingyesterday.domain.model.Rumor
import com.rainingyesterday.domain.model.Task

/**
 * 派生世界状态 DerivedState（docs/02 §4）：触发引擎在当前 WorldState + PlayerProgress 下算出的"此刻世界"。
 * UI 与 AR 视图（P2）只读这份快照，不再各自推理。不可变值对象。
 */
data class DerivedState(
    /** 当前时刻满足触发条件、可见的随机事件 */
    val visibleEvents: List<RandomEvent> = emptyList(),
    /** 当前满足时空条件的传闻（深层入口候选） */
    val activeRumors: List<Rumor> = emptyList(),
    /** NPC 当前位置：npcId -> 所在 place id（由作息表命中当前时间推出） */
    val npcPositions: Map<String, String> = emptyMap(),
    /** 当前可刷新的资源 item id 列表 */
    val spawnables: List<String> = emptyList(),
    /** 当前地点可执行的微仪式 */
    val availableRituals: List<Ritual> = emptyList(),
    /** 满足解锁条件、可接取的异常任务 */
    val availableAnomalies: List<Task> = emptyList(),
    /** 待处理的物品掉落效果（事件等产生） */
    val pendingEffects: List<Effect> = emptyList(),
    /** AR 可见虚拟物（MVP 只算不渲染，P2 消费）。列表为 {type, id, asset} */
    val arVisible: List<ArHint> = emptyList(),
) {
    val isEmpty: Boolean
        get() = visibleEvents.isEmpty() && activeRumors.isEmpty() && npcPositions.isEmpty() &&
            spawnables.isEmpty() && availableRituals.isEmpty() && availableAnomalies.isEmpty() &&
            pendingEffects.isEmpty() && arVisible.isEmpty()
}

/** AR 虚拟物提示（MVP 不渲染，仅占位；P2 bilboard 消费） */
data class ArHint(
    val kind: ArKind,
    val id: String,
    val asset: String,
)

enum class ArKind { PLACE, NPC, CREATURE }