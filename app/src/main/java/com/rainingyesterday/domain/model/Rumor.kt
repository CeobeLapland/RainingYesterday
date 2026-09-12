package com.rainingyesterday.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 传闻（docs/02_DATA_MODEL.md §2.7）：深层入口，蹲守成功或落空 */
@Serializable
data class Rumor(
    val id: String,
    val name: String,
    val text: String,
    val condition: List<Condition> = emptyList(),
    val resolve: String? = null,
    @SerialName("may_fail")
    val mayFail: Boolean = false,
    val evidence: String? = null,
)