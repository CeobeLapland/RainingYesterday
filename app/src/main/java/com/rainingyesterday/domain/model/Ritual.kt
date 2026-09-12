package com.rainingyesterday.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 地点微仪式（docs/02_DATA_MODEL.md §2.10）：地点级一次性/低频轻事件 */
@Serializable
data class Ritual(
    val id: String,
    val name: String,
    val place: String,
    val action: String? = null,
    @SerialName("cooldown_days")
    val cooldownDays: Int = 0,
    val effects: List<Effect> = emptyList(),
)