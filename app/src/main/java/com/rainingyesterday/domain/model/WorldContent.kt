package com.rainingyesterday.domain.model

import kotlinx.serialization.Serializable

/**
 * 主题 / 地图包（docs/02_DATA_MODEL.md §2.8）。
 * 一组内容文件的清单；files 是 "类型名 -> 文件名" 映射，缺省的文件优雅跳过。
 */
@Serializable
data class Theme(
    val id: String,
    val name: String = "",
    val version: Int = 1,
    val files: Map<String, String> = emptyMap(),
)

/** 一个主题包解析出的全部世界内容，供 [com.rainingyesterday.domain.repository.ContentRepository] 使用 */
@Serializable
data class WorldContent(
    val theme: Theme,
    val places: List<Place> = emptyList(),
    val npcs: List<Npc> = emptyList(),
    val creatures: List<Creature> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val items: List<Item> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
    val events: List<RandomEvent> = emptyList(),
    val rumors: List<Rumor> = emptyList(),
    val rituals: List<Ritual> = emptyList(),
    val festivals: List<Festival> = emptyList(),
    val tools: List<Tool> = emptyList(),
)