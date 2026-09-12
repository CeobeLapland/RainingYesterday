package com.rainingyesterday.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * 触发引擎的统一输入，一次对世界/玩家状态的谓词判断（docs/02_DATA_MODEL.md §1）。
 *
 * 同时支持两种 JSON 形态：
 * - 单一条件：`{ "kind": "time_range", "value": { from, to } }`
 * - 组合条件：`{ "all": ... }`（AND）、`{ "any": ... }`（OR），键为 all / any
 *
 * 由于单一条件的 value 结构随 kind 变化、且组合条件无 kind 字段，
 * 无法用 kotlinx 多态直接表达，故用 [ConditionSerializer]（经 @Serializable(with=) 指定）经 JsonElement 手工分发。
 */
@Serializable(with = ConditionSerializer::class)
sealed interface Condition

/** AND 组合 */
@Serializable
data class AllCondition(val conditions: List<Condition>) : Condition

/** OR 组合 */
@Serializable
data class AnyCondition(val conditions: List<Condition>) : Condition

/** 单一条件的公共基类（kind 分发见 [ConditionSerializer]） */
sealed interface SingleCondition : Condition

/** 当日时间区间 */
@Serializable
@SerialName("time_range")
data class TimeRangeCondition(val from: String, val to: String) : SingleCondition

/** 天气匹配 */
@Serializable
@SerialName("weather")
data class WeatherCondition(val weather: Weather) : SingleCondition

/** 季节匹配 */
@Serializable
@SerialName("season")
data class SeasonCondition(val season: Season) : SingleCondition

/** 距某地点半径内 */
@Serializable
@SerialName("near")
data class NearCondition(val place: String, val radiusM: Double) : SingleCondition

/** 探索度 ≥ min（0~1） */
@Serializable
@SerialName("exploration")
data class ExplorationCondition(val min: Double) : SingleCondition

/** 熟悉度 ≥ min */
@Serializable
@SerialName("relationship")
data class RelationshipCondition(val npc: String, val min: Int) : SingleCondition

/** 持有物数量 ≥ min */
@Serializable
@SerialName("has_item")
data class HasItemCondition(val item: String, val min: Int) : SingleCondition

/** 星期（1=周一） */
@Serializable
@SerialName("day_of_week")
data class DayOfWeekCondition(val days: List<Int>) : SingleCondition

/** 具体日期（节日） */
@Serializable
@SerialName("date")
data class DateCondition(val month: Int, val day: Int) : SingleCondition

/** 条件的自定义序列化：JSON 双向（KSerializer），支持 kind/value 与 all/any。 */
object ConditionSerializer : KSerializer<Condition> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Condition")

    override fun deserialize(decoder: Decoder): Condition {
        val json = decoder as? JsonDecoder ?: throw SerializationException("Condition 只支持 JSON 序列化")
        return json.decodeJsonElement().toCondition()
    }

    override fun serialize(encoder: Encoder, value: Condition) {
        val json = encoder as? JsonEncoder ?: throw SerializationException("Condition 只支持 JSON 序列化")
        json.encodeJsonElement(value.toJsonElement())
    }

    // ---------- 反序列化 JsonElement -> Condition ----------

    private fun JsonElement.toCondition(): Condition = when {
        this is JsonObject && containsKey("all") ->
            AllCondition(allArray("all").map { it.toCondition() })
        this is JsonObject && containsKey("any") ->
            AnyCondition(allArray("any").map { it.toCondition() })
        this is JsonObject && containsKey("kind") -> parseSingle(this)
        else -> throw SerializationException("无法识别的 Condition：$this")
    }

    private fun JsonObject.allArray(key: String): JsonArray {
        val v = this[key] ?: throw SerializationException("Condition 缺少 $key 数组")
        return v as? JsonArray ?: throw SerializationException("Condition.$key 应为数组")
    }

    private fun parseSingle(obj: JsonObject): SingleCondition {
        val kind = (obj["kind"] as? JsonPrimitive)?.takeIf { it.isString }?.content
            ?: throw SerializationException("Condition 缺少 kind")
        val value = obj["value"] ?: throw SerializationException("Condition.kind=$kind 缺少 value")
        return when (kind) {
            "time_range" -> {
                val v = value.jsonObject
                TimeRangeCondition(
                    from = prim(v["from"]) ?: err(kind, "from"),
                    to = prim(v["to"]) ?: err(kind, "to"),
                )
            }
            "weather" -> WeatherCondition(pick<Weather>(prim(value)) ?: err(kind, "value"))
            "season" -> SeasonCondition(pick<Season>(prim(value)) ?: err(kind, "value"))
            "near" -> {
                val v = value.jsonObject
                NearCondition(
                    place = prim(v["place"]) ?: err(kind, "place"),
                    radiusM = dbl(v["radius_m"]) ?: err(kind, "radius_m"),
                )
            }
            "exploration" -> ExplorationCondition(dbl(value.jsonObject["min"]) ?: err(kind, "min"))
            "relationship" -> {
                val v = value.jsonObject
                RelationshipCondition(npc = prim(v["npc"]) ?: err(kind, "npc"), min = int(v["min"]) ?: err(kind, "min"))
            }
            "has_item" -> {
                val v = value.jsonObject
                HasItemCondition(item = prim(v["item"]) ?: err(kind, "item"), min = int(v["min"]) ?: err(kind, "min"))
            }
            "day_of_week" -> DayOfWeekCondition(
                days = (value as JsonArray).map { (it as JsonPrimitive).content.toInt() },
            )
            "date" -> {
                val v = value.jsonObject
                DateCondition(month = int(v["month"]) ?: err(kind, "month"), day = int(v["day"]) ?: err(kind, "day"))
            }
            else -> throw SerializationException("未知 Condition kind：$kind")
        }
    }

    private fun prim(p: JsonElement?): String? = (p as? JsonPrimitive)?.takeIf { it.isString }?.content
    private fun dbl(p: JsonElement?): Double? = (p as? JsonPrimitive)?.let { it.content.toDoubleOrNull() }
    private fun int(p: JsonElement?): Int? = (p as? JsonPrimitive)?.let { it.content.toIntOrNull() }
    private fun err(kind: String, field: String): Nothing =
        throw SerializationException("Condition.kind=$kind 字段 $field 缺失或类型错误")
    private inline fun <reified T : Enum<T>> pick(raw: String?): T? =
        raw?.let { r -> enumValues<T>().firstOrNull { it.name.equals(r, ignoreCase = true) } }

    // ---------- 序列化 Condition -> JsonElement ----------

    private fun Condition.toJsonElement(): JsonElement = when (this) {
        is AllCondition -> buildJsonObject {
            putJsonArray("all") { conditions.forEach { add(it.toJsonElement()) } }
        }
        is AnyCondition -> buildJsonObject {
            putJsonArray("any") { conditions.forEach { add(it.toJsonElement()) } }
        }
        is TimeRangeCondition -> singleValue("time_range") {
            buildJsonObject { put("from", from); put("to", to) }
        }
        is WeatherCondition -> singleValue("weather") { JsonPrimitive(weather.name.lowercase()) }
        is SeasonCondition -> singleValue("season") { JsonPrimitive(season.name.lowercase()) }
        is NearCondition -> singleValue("near") {
            buildJsonObject { put("place", place); put("radius_m", radiusM) }
        }
        is ExplorationCondition -> singleValue("exploration") { buildJsonObject { put("min", min) } }
        is RelationshipCondition -> singleValue("relationship") { buildJsonObject { put("npc", npc); put("min", min) } }
        is HasItemCondition -> singleValue("has_item") { buildJsonObject { put("item", item); put("min", min) } }
        is DayOfWeekCondition -> singleValue("day_of_week") { buildJsonArray { days.forEach { add(JsonPrimitive(it)) } } }
        is DateCondition -> singleValue("date") { buildJsonObject { put("month", month); put("day", day) } }
    }

    private fun singleValue(kind: String, buildValue: () -> JsonElement): JsonObject = buildJsonObject {
        put("kind", kind)
        put("value", buildValue())
    }
}