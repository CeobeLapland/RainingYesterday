package com.rainingyesterday.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 生灵 / 友善生灵（docs/02_DATA_MODEL.md §2.9）：校园里可发现领养 */
@Serializable
data class Creature(
    val id: String,
    val name: String,
    val species: String = Species.OTHER.name.lowercase(),
    @SerialName("home_places")
    val homePlaces: List<String> = emptyList(),
    val rarity: Rarity = Rarity.COMMON,
    @SerialName("found_condition")
    val foundCondition: List<Condition> = emptyList(),
    val gifts: GiftPrefs? = null,
    val ar: ArSpec? = null,
)