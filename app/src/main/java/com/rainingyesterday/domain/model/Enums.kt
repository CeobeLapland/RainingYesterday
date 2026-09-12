package com.rainingyesterday.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 领域层枚举：全部对齐 docs/02_DATA_MODEL.md §5，由 JSON kind/value 映射而来。
 * 领域层保持纯 Kotlin，不得依赖 Android。任何新增 JSON 枚举都必须在这里落成 Kotlin 枚举。
 * 每个枚举值都用 @SerialName 标注 JSON 原文（小写），保证大小写敏感的反序列化不出错。
 */

/** 地点类型（02 §2.1 place.type） */
@Serializable
enum class PlaceType {
    @SerialName("building") BUILDING,
    @SerialName("nature") NATURE,
    @SerialName("public") PUBLIC,
    @SerialName("hidden") HIDDEN,
}

/** 物品六大类别资源（02 §2.4 item.category） */
@Serializable
enum class ItemCategory {
    @SerialName("material") MATERIAL,
    @SerialName("knowledge") KNOWLEDGE,
    @SerialName("memory") MEMORY,
    @SerialName("culture") CULTURE,
    @SerialName("trace") TRACE,
    @SerialName("story") STORY,
}

/** 稀有度（02 §2.4 item.rarity） */
@Serializable
enum class Rarity {
    @SerialName("common") COMMON,
    @SerialName("uncommon") UNCOMMON,
    @SerialName("rare") RARE,
    @SerialName("epic") EPIC,
    @SerialName("mystic") MYSTIC,
}

/** 任务类型：normal 普通 / anomaly 异常（02 §2.3 task.type） */
@Serializable
enum class TaskType {
    @SerialName("normal") NORMAL,
    @SerialName("anomaly") ANOMALY,
}

/** 随机事件档位（02 §2.6 event.tier） */
@Serializable
enum class EventTier {
    @SerialName("common") COMMON,
    @SerialName("uncommon") UNCOMMON,
    @SerialName("rare") RARE,
    @SerialName("epic") EPIC,
    @SerialName("mystic") MYSTIC,
}

/** 天气（02 §2.12 WeatherSignal.code） */
@Serializable
enum class Weather {
    @SerialName("sunny") SUNNY,
    @SerialName("cloudy") CLOUDY,
    @SerialName("rain") RAIN,
    @SerialName("snow") SNOW,
}

/** 季节（02 Condition/WorldState season） */
@Serializable
enum class Season {
    @SerialName("spring") SPRING,
    @SerialName("summer") SUMMER,
    @SerialName("autumn") AUTUMN,
    @SerialName("winter") WINTER,
}

/** AR 锚点模式（02 §2.11 ar.anchor_mode；P2 渲染时才用到） */
@Serializable
enum class ArAnchorMode {
    @SerialName("geo_anchor") GEO_ANCHOR,
    @SerialName("device_relative") DEVICE_RELATIVE,
}

/** 生灵物种（02 §2.9 creature.species） */
@Serializable
enum class Species {
    @SerialName("cat") CAT,
    @SerialName("bird") BIRD,
    @SerialName("squirrel") SQUIRREL,
    @SerialName("other") OTHER,
    @SerialName("unknown") UNKNOWN,
}