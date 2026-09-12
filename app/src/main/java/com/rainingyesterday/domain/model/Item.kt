package com.rainingyesterday.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 物品（docs/02_DATA_MODEL.md §2.4）：六类资源，可带稀有度、生成条件、配方引用 */
@Serializable
data class Item(
    val id: String,
    val name: String,
    val category: ItemCategory = ItemCategory.MATERIAL,
    val rarity: Rarity = Rarity.COMMON,
    @SerialName("spawn_places")
    val spawnPlaces: List<String> = emptyList(),
    @SerialName("spawn_condition")
    val spawnCondition: List<Condition> = emptyList(),
    @SerialName("used_in")
    val usedIn: List<String> = emptyList(),
    val ar: ArSpec? = null,
)