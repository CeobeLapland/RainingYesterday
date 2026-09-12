package com.rainingyesterday.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 节日（docs/02_DATA_MODEL.md §2.13，v2 新增）：校园真实节点，人工维护 */
@Serializable
data class Festival(
    val id: String,
    val name: String,
    val start: DateRef = DateRef(1, 1),
    val end: DateRef = DateRef(1, 1),
    val conditions: List<Condition> = emptyList(),
    @SerialName("resource_bonuses")
    val resourceBonuses: List<String> = emptyList(),
    @SerialName("npc_gathering")
    val npcGathering: List<String> = emptyList(),
    @SerialName("limited_content")
    val limitedContent: List<String> = emptyList(),
    val text: String = "",
)

/** 日期引用：{ month, day }，节日周期范围 */
@Serializable
data class DateRef(
    val month: Int,
    val day: Int,
)