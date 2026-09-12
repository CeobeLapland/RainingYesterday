package com.rainingyesterday.data.content

import com.rainingyesterday.domain.repository.ContentRepository
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
 * 测试用内存 [ContentRepository]，便于领域引擎单测（不依赖 assets / Android）。
 */
class FakeContentRepository(
    private val world: WorldContent,
) : ContentRepository {
    override fun content(): WorldContent = world
    override fun theme(): Theme = world.theme
    override fun places(): List<Place> = world.places
    override fun place(id: String): Place? = world.places.firstOrNull { it.id == id }
    override fun npcs(): List<Npc> = world.npcs
    override fun npc(id: String): Npc? = world.npcs.firstOrNull { it.id == id }
    override fun creatures(): List<Creature> = world.creatures
    override fun creature(id: String): Creature? = world.creatures.firstOrNull { it.id == id }
    override fun tasks(): List<Task> = world.tasks
    override fun task(id: String): Task? = world.tasks.firstOrNull { it.id == id }
    override fun items(): List<Item> = world.items
    override fun item(id: String): Item? = world.items.firstOrNull { it.id == id }
    override fun recipes(): List<Recipe> = world.recipes
    override fun recipe(id: String): Recipe? = world.recipes.firstOrNull { it.id == id }
    override fun events(): List<RandomEvent> = world.events
    override fun event(id: String): RandomEvent? = world.events.firstOrNull { it.id == id }
    override fun rumors(): List<Rumor> = world.rumors
    override fun rumor(id: String): Rumor? = world.rumors.firstOrNull { it.id == id }
    override fun rituals(): List<Ritual> = world.rituals
    override fun ritual(id: String): Ritual? = world.rituals.firstOrNull { it.id == id }
    override fun festivals(): List<Festival> = world.festivals
    override fun festival(id: String): Festival? = world.festivals.firstOrNull { it.id == id }
    override fun tools(): List<Tool> = world.tools
    override fun tool(id: String): Tool? = world.tools.firstOrNull { it.id == id }
}