package com.rainingyesterday.domain.model

import kotlinx.serialization.Serializable

/**
 * AR 通用可选字段（docs/02_DATA_MODEL.md §2.11）。
 * 出现在 place / npc / item / creature 上；缺省即不进 AR 渲染（MVP 只解析、不渲染）。
 * anchorMode 在 P2 前用字符串接收，P2 再落成 [ArAnchorMode] 枚举。
 */
@Serializable
data class ArSpec(
    val asset: String? = null,
    val anchorMode: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val scale: Double = 1.0,
    val billboard: Boolean = false,
) {
    val anchorModeEnum: ArAnchorMode?
        get() = anchorMode?.let { raw ->
            ArAnchorMode.entries.firstOrNull { it.name.equals(raw.replace('-', '_'), ignoreCase = true) }
        }
}