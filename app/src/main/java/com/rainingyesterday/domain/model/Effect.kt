package com.rainingyesterday.domain.model

import kotlinx.serialization.Serializable

/**
 * 通用效果条目：随机事件（event.effects）与地点微仪式（ritual.effects）共用。
 * kind：unlock_place / gain_item / create_memory / ...
 * 非死 schema——按 kind 读取所需字段，未知 kind 由校验器提示。
 */
@Serializable
data class Effect(
    val kind: String,
    val place: String? = null,
    val item: String? = null,
    val count: Int = 1,
    val text: String? = null,
)