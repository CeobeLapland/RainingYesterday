package com.rainingyesterday.domain.repository

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

/**
 * 世界内容仓库（docs/04 §4.1）。接口在 domain 定义、data 层实现。
 * 领域层只认接口，不碰 JSON / 文件细节。
 */
interface ContentRepository {

    /** 已载入的全部世界内容（一份，当前主题包） */
    fun content(): WorldContent

    fun theme(): Theme

    fun places(): List<Place>
    fun place(id: String): Place?

    fun npcs(): List<Npc>
    fun npc(id: String): Npc?

    fun creatures(): List<Creature>
    fun creature(id: String): Creature?

    fun tasks(): List<Task>
    fun task(id: String): Task?

    fun items(): List<Item>
    fun item(id: String): Item?

    fun recipes(): List<Recipe>
    fun recipe(id: String): Recipe?

    fun events(): List<RandomEvent>
    fun event(id: String): RandomEvent?

    fun rumors(): List<Rumor>
    fun rumor(id: String): Rumor?

    fun rituals(): List<Ritual>
    fun ritual(id: String): Ritual?

    fun festivals(): List<Festival>
    fun festival(id: String): Festival?

    fun tools(): List<Tool>
    fun tool(id: String): Tool?
}