package com.rainingyesterday.domain.model

import kotlinx.serialization.Serializable

/** 随机事件（docs/02_DATA_MODEL.md §2.6） */
@Serializable
data class RandomEvent(
    val id: String,
    val name: String,
    val tier: EventTier = EventTier.COMMON,
    val trigger: List<Condition> = emptyList(),
    val effects: List<Effect> = emptyList(),
    val text: String = "",
)