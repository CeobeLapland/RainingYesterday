package com.rainingyesterday.data.content

import kotlinx.serialization.json.Json

/**
 * 世界内容 JSON 解析的共享 Json 配置（docs/03 §5）。
 * ignoreUnknownKeys=true 保证多余字段不崩、向后兼容。
 * Condition 已通过 @Serializable(with = ConditionSerializer::class) 指定自己的 serializer，
 * 解析 List<Condition> 时会自动使用，无需在此额外注册。
 */
internal val ContentJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}