package com.rainingyesterday.domain.model

import kotlinx.serialization.Serializable

/** 工具升级线（docs/02_DATA_MODEL.md §2.14，v2 新增）：探索效率成长 */
@Serializable
data class Tool(
    val id: String,
    val name: String,
    val tiers: List<ToolTier> = emptyList(),
)

/** 工具档位：当前档的 effect + 升级到此档的花费 */
@Serializable
data class ToolTier(
    val level: Int,
    val name: String,
    val effect: Map<String, Double> = emptyMap(),
    val cost: Map<String, Int> = emptyMap(),
)