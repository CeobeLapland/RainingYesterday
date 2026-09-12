package com.rainingyesterday.domain.model

import kotlinx.serialization.Serializable

/** 真实信号数据形状（docs/02_DATA_MODEL.md §2.12）。非 JSON 内容，由 SignalSource 产出。 */

/** 真实天气信号 */
@Serializable
data class WeatherSignal(val code: Weather)

/** 真实昼夜日照信号 */
@Serializable
data class DaylightSignal(val sunrise: String, val sunset: String)

/** 校园实时动态信号（可人工维护兜底） */
@Serializable
data class CampusSignal(val events: List<CampusEvent> = emptyList())

/** 校园动态事件 */
@Serializable
data class CampusEvent(
    val id: String,
    val text: String,
    val place: String? = null,
)