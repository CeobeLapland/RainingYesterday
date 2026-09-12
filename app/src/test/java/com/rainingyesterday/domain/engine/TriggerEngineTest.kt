package com.rainingyesterday.domain.engine

import com.rainingyesterday.data.content.FakeContentRepository
import com.rainingyesterday.domain.model.AllCondition
import com.rainingyesterday.domain.model.AnyCondition
import com.rainingyesterday.domain.model.DateCondition
import com.rainingyesterday.domain.model.DayOfWeekCondition
import com.rainingyesterday.domain.model.Effect
import com.rainingyesterday.domain.model.EventTier
import com.rainingyesterday.domain.model.ExplorationCondition
import com.rainingyesterday.domain.model.HasItemCondition
import com.rainingyesterday.domain.model.Item
import com.rainingyesterday.domain.model.ItemCategory
import com.rainingyesterday.domain.model.NearCondition
import com.rainingyesterday.domain.model.Npc
import com.rainingyesterday.domain.model.NpcSchedule
import com.rainingyesterday.domain.model.Place
import com.rainingyesterday.domain.model.PlaceType
import com.rainingyesterday.domain.model.PlayerProgress
import com.rainingyesterday.domain.model.RandomEvent
import com.rainingyesterday.domain.model.Rarity
import com.rainingyesterday.domain.model.RelationshipCondition
import com.rainingyesterday.domain.model.Ritual
import com.rainingyesterday.domain.model.Rumor
import com.rainingyesterday.domain.model.Season
import com.rainingyesterday.domain.model.SeasonCondition
import com.rainingyesterday.domain.model.Task
import com.rainingyesterday.domain.model.TaskType
import com.rainingyesterday.domain.model.Theme
import com.rainingyesterday.domain.model.TimeRangeCondition
import com.rainingyesterday.domain.model.Weather
import com.rainingyesterday.domain.model.WeatherCondition
import com.rainingyesterday.domain.model.WorldContent
import com.rainingyesterday.domain.model.WorldState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** M2 验收：各 Condition kind + 组合嵌套 + 改天气/探索度使 DerivedState 正确变化。 */
class TriggerEngineTest {

    private val date = LocalDate.of(2026, 9, 12) // 周六（dayOfWeek==6），秋季

    private fun world(
        time: String = "22:00",
        weather: Weather = Weather.RAIN,
        season: Season = Season.AUTUMN,
        date: LocalDate = this.date,
    ) = WorldState(time = time, weather = weather, season = season, date = date)

    private val placeLake = Place("lake", "湖边", PlaceType.NATURE, 31.2, 121.4, 60.0)

    private fun repo(
        events: List<RandomEvent> = emptyList(),
        rumors: List<Rumor> = emptyList(),
        items: List<Item> = emptyList(),
        tasks: List<Task> = emptyList(),
        npcs: List<Npc> = emptyList(),
        rituals: List<Ritual> = emptyList(),
    ) = FakeContentRepository(
        WorldContent(
            theme = Theme("t"),
            places = listOf(placeLake),
            events = events, rumors = rumors, items = items, tasks = tasks, npcs = npcs, rituals = rituals,
        )
    )

    private fun item(cond: List<com.rainingyesterday.domain.model.Condition>) = Item(
        id = "mush", name = "菌", spawnCondition = cond,
    )

    // ---------- Condition 各 kind ----------

    @Test
    fun `time_range 判断命中`() {
        val player = PlayerProgress()
        val evaluator = ConditionEvaluator()
        assertTrue(evaluator.evaluate(TimeRangeCondition("21:00", "23:00"), world(), player))
        assertFalse(evaluator.evaluate(TimeRangeCondition("06:00", "08:00"), world(), player))
    }

    @Test
    fun `time_range 跨午夜区间`() {
        val evaluator = ConditionEvaluator()
        val w = world("01:00")
        assertTrue(evaluator.evaluate(TimeRangeCondition("22:00", "05:00"), w, PlayerProgress()))
    }

    @Test
    fun `weather season date day_of_week 判断`() {
        val evaluator = ConditionEvaluator()
        val w = world()
        assertTrue(evaluator.evaluate(WeatherCondition(Weather.RAIN), w, PlayerProgress()))
        assertTrue(evaluator.evaluate(SeasonCondition(Season.AUTUMN), w, PlayerProgress()))
        assertTrue(evaluator.evaluate(DateCondition(9, 12), w, PlayerProgress()))
        assertTrue(evaluator.evaluate(DayOfWeekCondition(listOf(6, 7)), w, PlayerProgress()))
    }

    @Test
    fun `exploration relationship has_item 判断`() {
        val evaluator = ConditionEvaluator()
        val w = world()
        val p = PlayerProgress(
            exploration = 0.7,
            relationships = mapOf("n1" to 3),
            inventory = mapOf("tea" to 5),
        )
        assertTrue(evaluator.evaluate(ExplorationCondition(0.6), w, p))
        assertFalse(evaluator.evaluate(ExplorationCondition(0.9), w, p))
        assertTrue(evaluator.evaluate(RelationshipCondition("n1", 2), w, p))
        assertTrue(evaluator.evaluate(HasItemCondition("tea", 3), w, p))
        assertFalse(evaluator.evaluate(HasItemCondition("gold", 1), w, p))
    }

    @Test
    fun `near 判断用经纬度距离`() {
        val evaluator = ConditionEvaluator(placeResolver = { id -> if (id == "lake") placeLake else null })
        val w = world()
        val near = PlayerProgress(lat = 31.2001, lng = 121.4001) // 距湖边很近
        val far = PlayerProgress(lat = 31.3, lng = 121.5)
        assertTrue(evaluator.evaluate(NearCondition("lake", 100.0), w, near))
        assertFalse(evaluator.evaluate(NearCondition("lake", 100.0), w, far))
        // 地点不存在 -> false
        assertFalse(evaluator.evaluate(NearCondition("nope", 100.0), w, near))
    }

    @Test
    fun `all 和 any 组合嵌套`() {
        val evaluator = ConditionEvaluator()
        val w = world()
        val p = PlayerProgress(exploration = 0.7)
        val compound = AllCondition(
            listOf(
                WeatherCondition(Weather.RAIN),
                AnyCondition(
                    listOf(
                        ExplorationCondition(0.9), // false
                        ExplorationCondition(0.5), // true
                    )
                ),
            )
        )
        assertTrue(evaluator.evaluate(compound, w, p))

        val mustFail = AllCondition(
            listOf(WeatherCondition(Weather.SUNNY), WeatherCondition(Weather.RAIN))
        )
        assertFalse(evaluator.evaluate(mustFail, w, p))
    }

    // ---------- TriggerEngine / DerivedState ----------

    @Test
    fun `天气从雨变晴 - spawnable 与活性传闻随天气变化`() {
        // 雨夜菌：条件 = 雨天 + 晚间
        val mushroom = item(listOf(WeatherCondition(Weather.RAIN), TimeRangeCondition("18:00", "23:00")))
        val rumorRain = Rumor("r1", "r", "雨天传闻", condition = listOf(WeatherCondition(Weather.RAIN)))
        val engine = TriggerEngine(repo(items = listOf(mushroom), rumors = listOf(rumorRain)))

        val rainy = engine.derive(world(time = "22:00", weather = Weather.RAIN), PlayerProgress())
        assertTrue(rainy.spawnables.contains("mush"))
        assertTrue(rainy.activeRumors.any { it.id == "r1" })

        // 改为晴天 -> 不再可刷新、传闻不激活
        val sunny = engine.derive(world(time = "22:00", weather = Weather.SUNNY), PlayerProgress())
        assertFalse(sunny.spawnables.contains("mush"))
        assertFalse(sunny.activeRumors.any { it.id == "r1" })
    }

    @Test
    fun `探索度不足不解锁异常任务 提升后解锁`() {
        val anomaly = Task(
            id = "a1", name = "异常", type = TaskType.ANOMALY,
            unlock = listOf(ExplorationCondition(0.6)),
        )
        val engine = TriggerEngine(repo(tasks = listOf(anomaly)))

        val low = engine.derive(world(), PlayerProgress(exploration = 0.3))
        assertTrue(low.availableAnomalies.isEmpty())

        val high = engine.derive(world(), PlayerProgress(exploration = 0.8))
        assertTrue(high.availableAnomalies.any { it.id == "a1" })
    }

    @Test
    fun `随机事件 trigger 命中才进入 visibleEvents 且效果累积`() {
        val event = RandomEvent(
            id = "cat", name = "猫", tier = EventTier.RARE,
            trigger = listOf(NearCondition("lake", 100.0)),
            effects = listOf(Effect("gain_item", item = "dried_fish", count = 1)),
        )
        val engine = TriggerEngine(repo(events = listOf(event)))

        val atLake = engine.derive(world(), PlayerProgress(lat = 31.2001, lng = 121.4001))
        assertTrue(atLake.visibleEvents.any { it.id == "cat" })
        assertTrue(atLake.pendingEffects.any { it.item == "dried_fish" })

        val away = engine.derive(world(), PlayerProgress(lat = 31.3, lng = 121.5))
        assertFalse(away.visibleEvents.any { it.id == "cat" })
    }

    @Test
    fun `NPC 作息命中当前位置`() {
        val npc = Npc(
            id = "li", name = "李",
            schedule = listOf(NpcSchedule(from = "09:00", to = "12:00", place = "lake")),
        )
        val engine = TriggerEngine(repo(npcs = listOf(npc)))
        val pos = engine.derive(world(time = "10:00"), PlayerProgress()).npcPositions
        assertEquals("lake", pos["li"])
        val nobody = engine.derive(world(time = "14:00"), PlayerProgress()).npcPositions
        assertFalse(nobody.containsKey("li"))
    }

    @Test
    fun `微仪式仅在玩家位于对应地点时可执行`() {
        val ritual = Ritual(id = "sit", name = "坐", place = "lake")
        val engine = TriggerEngine(repo(rituals = listOf(ritual)))
        assertTrue(engine.derive(world(), PlayerProgress(currentPlaceId = "lake")).availableRituals.any { it.id == "sit" })
        assertTrue(engine.derive(world(), PlayerProgress(currentPlaceId = null)).availableRituals.isEmpty())
    }
}