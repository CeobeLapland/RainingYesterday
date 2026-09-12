package com.rainingyesterday.data.content

import android.content.Context
import com.rainingyesterday.domain.model.Creature
import com.rainingyesterday.domain.model.Festival
import com.rainingyesterday.domain.model.Item
import com.rainingyesterday.domain.model.Npc
import com.rainingyesterday.domain.model.Place
import com.rainingyesterday.domain.model.RandomEvent
import com.rainingyesterday.domain.model.Recipe
import com.rainingyesterday.domain.model.Ritual
import com.rainingyesterday.domain.model.Rumor
import com.rainingyesterday.domain.model.Task
import com.rainingyesterday.domain.model.Theme
import com.rainingyesterday.domain.model.Tool
import com.rainingyesterday.domain.model.WorldContent
import com.rainingyesterday.domain.repository.ContentRepository
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 基于 assets 的 [ContentRepository] 实现（docs/03 §5，content 随包发布）。
 * 首次解析时同步载入全部内容并缓存在内存；坏 JSON / 校验失败在首次访问时显式抛错。
 */
@Singleton
class AssetContentRepository @Inject constructor(
    private val context: Context,
    private val parser: JsonWorldContentParser = JsonWorldContentParser(),
    private val validator: ContentValidator = ContentValidator(),
) : ContentRepository {

    private val themeId = "campus_default"
    private val contentDir = "content/$themeId"

    private val content: WorldContent by lazy {
        val files = loadThemeFiles()
        val parsed = parser.parse(files)
        validator.validate(parsed)
        parsed
    }

    /** 读取主题包所有文件文本，返回 "文件名 -> 文本"。 */
    private fun loadThemeFiles(): Map<String, String> {
        val themeJson = context.assets.open("$contentDir/theme.json")
            .bufferedReader().use { it.readText() }

        val result = mutableMapOf("theme.json" to themeJson)

        // theme.files 声明的文件名（值为文件名），逐一读取；缺失则跳过（兼容，空列表兜底）
        val declaredFilenames = ThemeJson.peekFileMap(themeJson).orEmpty().values.toSet()
        declaredFilenames.forEach { name ->
            readAssetOrNull(name)?.let { result[name] = it }
        }

        // 兜底：对 docs/02 约定的每个类型，若未声明或读取失败，尝试默认文件名，缺则跳过
        DEFAULT_FILENAMES.forEach { name ->
            if (name !in result) readAssetOrNull(name)?.let { result[name] = it }
        }
        return result
    }

    private fun readAssetOrNull(name: String): String? =
        try {
            context.assets.open("$contentDir/$name").bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        }

    private companion object {
        val DEFAULT_FILENAMES = listOf(
            "theme.json", "places.json", "npcs.json", "creatures.json", "tasks.json",
            "items.json", "recipes.json", "events.json", "rumors.json",
            "rituals.json", "festivals.json", "tools.json",
        )
    }

    override fun content(): WorldContent = content
    override fun theme(): Theme = content.theme
    override fun places(): List<Place> = content.places
    override fun place(id: String): Place? = content.places.firstOrNull { it.id == id }
    override fun npcs(): List<Npc> = content.npcs
    override fun npc(id: String): Npc? = content.npcs.firstOrNull { it.id == id }
    override fun creatures(): List<Creature> = content.creatures
    override fun creature(id: String): Creature? = content.creatures.firstOrNull { it.id == id }
    override fun tasks(): List<Task> = content.tasks
    override fun task(id: String): Task? = content.tasks.firstOrNull { it.id == id }
    override fun items(): List<Item> = content.items
    override fun item(id: String): Item? = content.items.firstOrNull { it.id == id }
    override fun recipes(): List<Recipe> = content.recipes
    override fun recipe(id: String): Recipe? = content.recipes.firstOrNull { it.id == id }
    override fun events(): List<RandomEvent> = content.events
    override fun event(id: String): RandomEvent? = content.events.firstOrNull { it.id == id }
    override fun rumors(): List<Rumor> = content.rumors
    override fun rumor(id: String): Rumor? = content.rumors.firstOrNull { it.id == id }
    override fun rituals(): List<Ritual> = content.rituals
    override fun ritual(id: String): Ritual? = content.rituals.firstOrNull { it.id == id }
    override fun festivals(): List<Festival> = content.festivals
    override fun festival(id: String): Festival? = content.festivals.firstOrNull { it.id == id }
    override fun tools(): List<Tool> = content.tools
    override fun tool(id: String): Tool? = content.tools.firstOrNull { it.id == id }
}

/** theme.json 的轻量解析，仅取 files 映射用于驱动 assets 读取。 */
private object ThemeJson {
    fun peekFileMap(themeJson: String): Map<String, String>? = try {
        val node = kotlinx.serialization.json.Json.parseToJsonElement(themeJson)
        val files = (node as? kotlinx.serialization.json.JsonObject)?.get("files")
            ?: return null
        (files as? kotlinx.serialization.json.JsonObject)?.let { obj ->
            obj.mapValues { (_, v) -> v.jsonPrimitive.content }
        }
    } catch (_: Exception) {
        null
    }
}