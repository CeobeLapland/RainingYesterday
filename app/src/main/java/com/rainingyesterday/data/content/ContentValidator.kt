package com.rainingyesterday.data.content

import com.rainingyesterday.domain.model.WorldContent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 世界内容的合法性校验（docs/04 §4.2）。
 * 内容载入后做一次校验：id 唯一、跨文件引用存在、类型合法。
 * 坏数据在启动时显式报错（[IllegalStateException]），绝不默默吞掉。
 */
@Singleton
class ContentValidator @Inject constructor() {

    fun validate(content: WorldContent) {
        val errors = mutableListOf<String>()
        val base = "${content.theme.id}（v${content.theme.version}）"

        // 1) 各集合内部 id 唯一 + 非空
        fun checkUnique(tag: String, ids: List<String>) {
            val duplicated = ids.groupingBy { it }.eachCount().filter { it.value > 1 }
            duplicated.forEach { (id, count) -> errors += "$base $tag 存在重复 id $id（$count 次）" }
            ids.filter { it.isBlank() }.forEach { errors += "$base $tag 存在空 id" }
        }
        checkUnique("地点", content.places.map { it.id })
        checkUnique("人物", content.npcs.map { it.id })
        checkUnique("生灵", content.creatures.map { it.id })
        checkUnique("任务", content.tasks.map { it.id })
        checkUnique("物品", content.items.map { it.id })
        checkUnique("配方", content.recipes.map { it.id })
        checkUnique("随机事件", content.events.map { it.id })
        checkUnique("传闻", content.rumors.map { it.id })
        checkUnique("微仪式", content.rituals.map { it.id })
        checkUnique("节日", content.festivals.map { it.id })
        checkUnique("工具", content.tools.map { it.id })

        val placeIds = content.places.map { it.id }.toSet()
        val npcIds = content.npcs.map { it.id }.toSet()
        val itemIds = content.items.map { it.id }.toSet()
        val rumorIds = content.rumors.map { it.id }.toSet()
        val creatureIds = content.creatures.map { it.id }.toSet()

        // 2) 跨文件引用存在（用"报告最多前若干条"避免刷屏）
        content.places.forEach { p ->
            p.resources.forEach { id -> if (id !in itemIds) errors += "$base 地点 ${p.id} 引用了不存在的物品 $id" }
            p.linkedNpcs.forEach { id -> if (id !in npcIds) errors += "$base 地点 ${p.id} 引用了不存在的人物 $id" }
            p.linkedRumors.forEach { id -> if (id !in rumorIds) errors += "$base 地点 ${p.id} 引用了不存在的传闻 $id" }
        }
        content.npcs.forEach { n ->
            (n.home?.let { listOf(it) } ?: emptyList()).plus(n.schedule.map { it.place })
                .forEach { id -> if (id !in placeIds) errors += "$base 人物 ${n.id} 引用了不存在的地点 $id" }
            (n.gifts?.liked.orEmpty() + n.gifts?.disliked.orEmpty())
                .forEach { id -> if (id !in itemIds) errors += "$base 人物 ${n.id} 收藏引用了不存在的物品 $id" }
        }
        content.creatures.forEach { c ->
            c.homePlaces.forEach { id -> if (id !in placeIds) errors += "$base 生灵 ${c.id} 引用了不存在的地点 $id" }
            (c.gifts?.liked.orEmpty() + c.gifts?.disliked.orEmpty())
                .forEach { id -> if (id !in itemIds) errors += "$base 生灵 ${c.id} 收藏引用了不存在的物品 $id" }
        }
        content.items.forEach { i ->
            i.spawnPlaces.forEach { id -> if (id !in placeIds) errors += "$base 物品 ${i.id} 的生成地不存在 $id" }
        }
        content.rituals.forEach { r ->
            if (r.place !in placeIds) errors += "$base 微仪式 ${r.id} 引用了不存在的地点 ${r.place}"
        }
        content.recipes.forEach { r ->
            (r.inputs.keys + r.output.item).forEach { id ->
                if (id !in itemIds) errors += "$base 配方 ${r.id} 引用了不存在的物品 $id"
            }
        }
        content.tasks.forEach { t ->
            t.objectives.forEach { o ->
                o.place?.let { if (it !in placeIds) errors += "$base 任务 ${t.id} 目标引用了不存在的地点 $it" }
                o.item?.let { if (it !in itemIds) errors += "$base 任务 ${t.id} 目标引用了不存在的物品 $it" }
            }
            t.rewards?.unlockHistory?.let { if (it !in placeIds) errors += "$base 任务 ${t.id} 解锁了不存在的地点 $it" }
            t.rewards?.unlockRumor?.let { if (it !in rumorIds) errors += "$base 任务 ${t.id} 解锁了不存在的传闻 $it" }
        }
        content.rumors.forEach { r ->
            r.resolve?.takeIf { it.isNotBlank() }?.let {
                if (it.startsWith("anomaly:") && !content.tasks.any { task -> task.id == it.substringAfter(':') }) {
                    errors += "$base 传闻 ${r.id} resolve 指向不存在的异常任务 $it"
                }
            }
            r.evidence?.let { if (it !in itemIds) errors += "$base 传闻 ${r.id} 的 evidence 不是已知物品 $it" }
        }
        content.festivals.forEach { f ->
            f.resourceBonuses.forEach { id -> if (id !in itemIds) errors += "$base 节日 ${f.id} 限定资源不存在 $id" }
            f.npcGathering.forEach { id -> if (id !in npcIds) errors += "$base 节日 ${f.id} 聚集人物不存在 $id" }
        }
        content.tools.forEach { t ->
            val levels = t.tiers.map { it.level }
            if (levels.size != levels.toSet().size) errors += "$base 工具 ${t.id} 的档位 level 重复"
            t.tiers.forEach { tier -> tier.cost.keys.forEach { it.let { id -> if (id !in itemIds) errors += "$base 工具 ${t.id} 升级花费引用了不存在物品 $id" } } }
        }

        if (errors.isNotEmpty()) {
            throw WorldContentValidationException(errors)
        }
    }
}

/** 世界内容校验失败：聚合全部问题一起抛出，便于一次看清。 */
class WorldContentValidationException(val errors: List<String>) :
    IllegalStateException("世界内容校验失败：" + errors.joinToString("；"))