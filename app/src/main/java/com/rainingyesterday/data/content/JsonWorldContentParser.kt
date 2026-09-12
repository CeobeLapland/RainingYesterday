package com.rainingyesterday.data.content

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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString

/**
 * 把一整套内容文件解析成 [WorldContent]。
 *
 * 输入是 "文件名 -> 文件文本" 的映射（由调用方从 assets 或测试资源装载），
 * 因此本类保持纯 Kotlin、可独立单测，不直接依赖 Android。
 *
 * 规则（docs/02 §2.8 / docs/04 §4.2）：
 * - 以 theme.json 为入口，读取其 files 映射来定位每个内容文件。
 * - files 未列出的类型不强求存在，缺省即空列表（优雅跳过，兼容旧主题包）。
 * - 解析失败抛出 [SerializationException]（含主题包与文件信息），启动时显式报错。
 */
class JsonWorldContentParser(
    private val json: kotlinx.serialization.json.Json = ContentJson,
) {

    /** 载入主题包：读 theme.json 入口，按其 files 映射解析各内容文件。 */
    fun parse(files: Map<String, String>): WorldContent {
        val theme = decodeTheme(files)
        val resolver = FileResolver(theme.id, theme.files, files, json)
        return WorldContent(
            theme = theme,
            places = resolver.decodeList<Place>("places"),
            npcs = resolver.decodeList<Npc>("npcs"),
            creatures = resolver.decodeList<Creature>("creatures"),
            tasks = resolver.decodeList<Task>("tasks"),
            items = resolver.decodeList<Item>("items"),
            recipes = resolver.decodeList<Recipe>("recipes"),
            events = resolver.decodeList<RandomEvent>("events"),
            rumors = resolver.decodeList<Rumor>("rumors"),
            rituals = resolver.decodeList<Ritual>("rituals"),
            festivals = resolver.decodeList<Festival>("festivals"),
            tools = resolver.decodeList<Tool>("tools"),
        )
    }

    private fun decodeTheme(files: Map<String, String>): Theme {
        val text = files[THEME_FILE] ?: throw SerializationException("主题包缺少入口文件 $THEME_FILE")
        return try {
            json.decodeFromString<Theme>(text)
        } catch (e: Exception) {
            throw SerializationException("解析入口文件 $THEME_FILE 失败：${e.message ?: e::class.simpleName}", e)
        }
    }

    private class FileResolver(
        private val themeId: String,
        private val fileMap: Map<String, String>,
        private val files: Map<String, String>,
        private val json: kotlinx.serialization.json.Json,
    ) {
        inline fun <reified T> decodeList(typeKey: String): List<T> {
            val fileName = fileMap[typeKey] ?: return emptyList() // 未声明该类型，优雅跳过
            val text = files[fileName]
                ?: throw SerializationException("主题 $themeId 声明了 $fileName，但未提供该文件内容")
            return try {
                json.decodeFromString<List<T>>(text)
            } catch (e: Exception) {
                throw SerializationException("解析 $themeId/$fileName 失败：${e.message ?: e::class.simpleName}", e)
            }
        }
    }

    private companion object {
        const val THEME_FILE = "theme.json"
    }
}