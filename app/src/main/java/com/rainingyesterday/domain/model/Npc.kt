package com.rainingyesterday.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 人物 NPC（docs/02_DATA_MODEL.md §2.2） */
@Serializable
data class Npc(
    val id: String,
    val name: String,
    val role: String? = null,
    val home: String? = null,
    val schedule: List<NpcSchedule> = emptyList(),
    val dialogue: List<DialogueTier> = emptyList(),
    val gifts: GiftPrefs? = null,
    val api: NpcApi? = null,
    val ar: ArSpec? = null,
)

/** NPC 作息段：在场时间 + 位置 + 出现概率 */
@Serializable
data class NpcSchedule(
    val from: String,
    val to: String,
    val place: String,
    val chance: Double = 1.0,
)

/** 对话档位：熟悉度 ≥ min_familiarity 时解锁这些台词 */
@Serializable
data class DialogueTier(
    @SerialName("min_familiarity")
    val minFamiliarity: Int,
    val lines: List<String> = emptyList(),
)

/** 礼物偏好 */
@Serializable
data class GiftPrefs(
    val liked: List<String> = emptyList(),
    val disliked: List<String> = emptyList(),
)

/** LLM API 通道（可选、默认关闭，预设对话兜底）。双轨共用同一数据结构。 */
@Serializable
data class NpcApi(
    val enabled: Boolean = false,
    @SerialName("base_url")
    val baseUrl: String = "",
    val model: String = "",
)