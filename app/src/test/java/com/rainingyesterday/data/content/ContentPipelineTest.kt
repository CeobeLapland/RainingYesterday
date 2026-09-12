package com.rainingyesterday.data.content

import com.rainingyesterday.domain.model.AllCondition
import com.rainingyesterday.domain.model.AnyCondition
import com.rainingyesterday.domain.model.Condition
import com.rainingyesterday.domain.model.DayOfWeekCondition
import com.rainingyesterday.domain.model.NearCondition
import com.rainingyesterday.domain.model.PlaceType
import com.rainingyesterday.domain.model.TimeRangeCondition
import com.rainingyesterday.domain.model.WorldContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * 数据管线单元测试（docs/05 M1 验收）：
 * 1) 能载入示例包（含全部内容类型与各类 Condition）
 * 2) 坏 JSON 启动时报错且信息可读
 * 3) 多余字段不崩（ignoreUnknownKeys）
 */
class ContentPipelineTest {

    private val parser = JsonWorldContentParser()

    /** 从 src/main/assets 读取真实主题包文件，验证 asset 内容与解析器可用。 */
    private fun loadRealAssets(): Map<String, String> {
        val dir = File("src/main/assets/content/campus_default")
        require(dir.exists()) { "找不到主题包目录：${dir.absolutePath}" }
        return dir.listFiles()
            ?.filter { it.isFile && it.extension == "json" }
            ?.associate { it.name to it.readText() }
            ?: emptyMap()
    }

    @Test
    fun `真实主题包可载入且引用合法`() {
        val files = loadRealAssets()
        val content = parser.parse(files)
        ContentValidator().validate(content) // 不应抛异常
        assertEquals("campus_default", content.theme.id)
        assertTrue(content.places.size >= 3)
        assertTrue(content.npcs.size >= 2)
        assertTrue(content.items.size >= 3)
        assertTrue(content.tasks.size >= 2)
        assertTrue(content.rumors.size >= 1)
        assertTrue(content.events.isNotEmpty())
        assertTrue(content.festivals.isNotEmpty())
        assertTrue(content.tools.isNotEmpty())
        assertTrue(content.rituals.isNotEmpty())
        assertTrue(content.creatures.isNotEmpty())
        assertTrue(content.recipes.isNotEmpty())
    }

    @Test
    fun `各类 Condition 均能解析`() {
        val files = mapOf(
            "theme.json" to """
                {
                  "id": "t", "name": "测试", "version": 1,
                  "files": { "npcs": "npcs.json", "events": "events.json", "items": "items.json" }
                }
            """.trimIndent(),
            "npcs.json" to """[]""",
            "events.json" to """[]""",
            "items.json" to """
                [{
                  "id": "mush", "name": "菌",
                  "category": "material", "rarity": "rare",
                  "spawn_places": ["p1"],
                  "spawn_condition": [
                    { "kind": "weather", "value": "rain" },
                    { "kind": "time_range", "value": { "from": "18:00", "to": "23:00" } },
                    { "all": 
                      [ { "kind": "near", "value": { "place": "p1", "radius_m": 50 } },
                        { "any": [ { "kind": "day_of_week", "value": [1,3,5] }, { "kind": "season", "value": "autumn" } ] } ]
                    }
                  ]
                }]
            """.trimIndent(),
        )
        val content = parser.parse(files)
        val conds = content.items.first().spawnCondition
        assertEquals(3, conds.size)
        assertTrue(conds[1] is TimeRangeCondition)
        val nested = conds[2] as AllCondition
        assertEquals(2, nested.conditions.size)
        assertTrue(nested.conditions[0] is NearCondition)
        assertTrue(nested.conditions[1] is AnyCondition)
        val any = nested.conditions[1] as AnyCondition
        assertTrue(any.conditions[0] is DayOfWeekCondition)
        assertEquals(listOf(1, 3, 5), (any.conditions[0] as DayOfWeekCondition).days)
    }

    @Test
    fun `多余字段不崩`() {
        val files = mapOf(
            "theme.json" to """{ "id":"t","file_extra":123,"files":{"places":"p.json"} }""",
            "p.json" to """
                [{ "id":"x","name":"X","type":"nature","lat":1.0,"lng":2.0,"radius_m":10.0,
                   "unknown_field":{"a":1},"pos":37}]
            """.trimIndent(),
        )
        val content = parser.parse(files)
        assertEquals(PlaceType.NATURE, content.places.first().type)
    }

    @Test
    fun `坏 JSON 时报错且指出文件`() {
        val files = mapOf(
            "theme.json" to """{ "id":"t","files":{"places":"p.json"} }""",
            "p.json" to """[{ "id": "x" }""", // 故意截断
        )
        try {
            parser.parse(files)
            fail("应抛解析异常")
        } catch (e: kotlinx.serialization.SerializationException) {
            assertTrue(e.message!!.contains("p.json"))
        }
    }

    @Test
    fun `引用不存在的物品时校验失败`() {
        val content = WorldContent(
            theme = parser.parse(
                mapOf("theme.json" to """{ "id":"t","files":{"places":"p.json"} }""", "p.json" to """[]"""),
            ).theme,
            places = listOf(com.rainingyesterday.domain.model.Place("p1", "P", PlaceType.BUILDING, 1.0, 2.0, 10.0, resources = listOf("ghost_item"))),
        )
        val errors = try {
            ContentValidator().validate(content)
            emptyList<String>()
        } catch (e: WorldContentValidationException) {
            e.errors
        }
        assertTrue(errors.any { it.contains("ghost_item") })
    }
}