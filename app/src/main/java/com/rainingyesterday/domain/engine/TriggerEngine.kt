package com.rainingyesterday.domain.engine

import com.rainingyesterday.domain.model.ArSpec
import com.rainingyesterday.domain.model.Effect
import com.rainingyesterday.domain.model.Npc
import com.rainingyesterday.domain.model.PlayerProgress
import com.rainingyesterday.domain.model.TaskType
import com.rainingyesterday.domain.model.WorldState
import com.rainingyesterday.domain.repository.ContentRepository
import com.rainingyesterday.domain.model.minuteOfDay

/**
 * 触发引擎（docs/01 §4.2）：按一组条件判定命中内容，产出 [DerivedState]。
 * 传闻 / 随机事件 / 异常解锁 / NPC 作息 / 稀有资源刷新 / 地点微仪式 / AR 可见物，
 * 全部由这一个引擎统一驱动，只靠内容数据不同来区分。
 *
 * 引擎不持有可变状态：输入 = 内容 + WorldState + PlayerProgress，输出 = 不可变 DerivedState 快照。
 */
class TriggerEngine(
    private val content: ContentRepository,
) {

    private val evaluator = ConditionEvaluator(placeResolver = { id -> content.place(id) })

    fun derive(world: WorldState, player: PlayerProgress): DerivedState {
        val events = content.events().filter { satisfied(it.trigger, world, player) }
        val rumors = content.rumors().filter { satisfied(it.condition, world, player) }
        val npcPositions = computeNpcPositions(world)
        val spawnables = content.items()
            .filter { it.spawnCondition.isNotEmpty() && satisfied(it.spawnCondition, world, player) }
            .map { it.id }
        val availableRituals = if (player.currentPlaceId == null) {
            emptyList()
        } else {
            content.rituals().filter { it.place == player.currentPlaceId }
        }
        val anomalies = content.tasks()
            .filter { it.type == TaskType.ANOMALY && satisfied(it.unlock, world, player) }

        val pendingEffects = events.flatMap { it.effects } + availableRituals.flatMap { it.effects }

        return DerivedState(
            visibleEvents = events,
            activeRumors = rumors,
            npcPositions = npcPositions,
            spawnables = spawnables,
            availableRituals = availableRituals,
            availableAnomalies = anomalies,
            pendingEffects = pendingEffects,
            arVisible = computeArVisible(world, player),
        )
    }

    private fun satisfied(conditions: List<com.rainingyesterday.domain.model.Condition>, world: WorldState, player: PlayerProgress): Boolean =
        conditions.isNotEmpty() && conditions.all { evaluator.evaluate(it, world, player) }

    /** NPC 作息命中当前时间：命中的每个 NPC 的当前地点。若多段命中取第一段。 */
    private fun computeNpcPositions(world: WorldState): Map<String, String> {
        val minute = world.minuteOfDay()
        return buildMap {
            content.npcs().forEach { npc: Npc ->
                npc.schedule.firstOrNull { inRange(minute, it.from, it.to) }?.let {
                    put(npc.id, it.place)
                }
            }
        }
    }

    private fun inRange(minute: Int, from: String, to: String): Boolean {
        val f = toMinutes(from); val t = toMinutes(to)
        return if (f <= t) minute in f..t else minute >= f || minute <= t
    }

    private fun toMinutes(hhmm: String): Int {
        val p = hhmm.split(":")
        return (p.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (p.getOrNull(1)?.toIntOrNull() ?: 0)
    }

    /** AR 虚拟物（MVP 只算不渲染，P2 消费）：当前地点的 AR + 附近生灵/NPC 的 AR。 */
    private fun computeArVisible(world: WorldState, player: PlayerProgress): List<ArHint> {
        val hints = mutableListOf<ArHint>()
        val currentPlace = player.currentPlaceId?.let { content.place(it) }
        currentPlace?.ar?.let { ar: ArSpec ->
            if (ar.asset != null) hints += ArHint(ArKind.PLACE, currentPlace.id, ar.asset)
        }
        // NPC：位于玩家当前位置点且配置了 AR 的 NPC
        content.npcs().forEach { npc ->
            val atCurrentPlace = player.currentPlaceId != null &&
                npc.schedule.any { it.place == player.currentPlaceId }
            if (atCurrentPlace) {
                npc.ar?.let { ar ->
                    if (!ar.asset.isNullOrBlank()) hints += ArHint(ArKind.NPC, npc.id, ar.asset)
                }
            }
        }
        // 生灵：满足出现条件且（未定位或家在当前位置）的刚物 AR
        content.creatures().forEach { creature ->
            val found = creature.foundCondition.isNotEmpty() && satisfied(creature.foundCondition, world, player)
            val reaches = player.currentPlaceId == null || creature.homePlaces.contains(player.currentPlaceId)
            if (found && reaches) {
                creature.ar?.let { ar ->
                    if (!ar.asset.isNullOrBlank()) hints += ArHint(ArKind.CREATURE, creature.id, ar.asset)
                }
            }
        }
        return hints
    }
}