package com.rainingyesterday.domain.model

import kotlinx.serialization.Serializable

/** 图鉴条目（docs/02 §3 EncyclopediaEntry 的领域形态）：refType+refId 指向世界内容 id，不复制内容。 */
@Serializable
data class EncyclopediaEntry(
    val refType: EncyclopediaType,
    val refId: String,
    val unlockedAt: Long = System.currentTimeMillis(),
)

/** 图鉴类型（对应用于地点/自然/人物/历史/传闻/记忆等图鉴分组）。 */
@Serializable
enum class EncyclopediaType { PLACE, NATURE, PERSON, HISTORY, RUMOR, MEMORY }

/** 足迹点（docs/02 §3 足迹 / exploration Route 的基础元素）。 */
@Serializable
data class TrackPoint(
    val lat: Double,
    val lng: Double,
    val timestampMs: Long = System.currentTimeMillis(),
)

/** 一条发现记录：何时在何地发现某内容（用于"[发现]"回放与足迹高亮）。 */
@Serializable
data class DiscoveryRecord(
    val refId: String,
    val lat: Double,
    val lng: Double,
    val timestampMs: Long = System.currentTimeMillis(),
)