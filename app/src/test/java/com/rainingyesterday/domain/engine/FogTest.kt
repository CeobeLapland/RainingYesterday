package com.rainingyesterday.domain.engine

import com.rainingyesterday.domain.model.FogRevealLayer
import com.rainingyesterday.domain.model.HexGrid
import com.rainingyesterday.domain.model.LocationMode
import com.rainingyesterday.domain.model.MapLayer
import com.rainingyesterday.domain.model.Place
import com.rainingyesterday.domain.model.PlaceType
import com.rainingyesterday.domain.model.PlayerProgress
import com.rainingyesterday.domain.model.PlayerSettings
import com.rainingyesterday.data.content.FakeContentRepository
import com.rainingyesterday.domain.model.Theme
import com.rainingyesterday.domain.model.WorldContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** M3 领域迷雾核心单测：六边形拓扑 + 解锁 + 分层显现 + 设置默认。 */
class FogTest {

    // ---------- HexGrid 拓扑 ----------

    @Test
    fun `半径 0 只含原点`() {
        val hexes = HexGrid.hexesWithinRadius(HexGrid.Axial(0, 0), 0)
        assertEquals(1, hexes.size)
        assertEquals("0:0", hexes.first().key)
    }

    @Test
    fun `半径 1 含 7 个格子`() {
        val hexes = HexGrid.hexesWithinRadius(HexGrid.Axial(0, 0), 1)
        assertEquals(7, hexes.size)
    }

    @Test
    fun `半径 2 含 19 个格子`() {
        val hexes = HexGrid.hexesWithinRadius(HexGrid.Axial(0, 0), 2)
        assertEquals(19, hexes.size)
    }

    @Test
    fun `六邻坐标正确`() {
        val neighbors = HexGrid.neighbors(HexGrid.Axial(0, 0))
        assertEquals(6, neighbors.size)
        // 六个方向一一对应
        val expected = setOf(
            "1:0", "1:-1", "0:-1", "-1:0", "-1:1", "0:1",
        )
        assertEquals(expected, neighbors.map { it.key }.toSet())
    }

    @Test
    fun `轴向距离`() {
        assertEquals(0, HexGrid.distance(HexGrid.Axial(0, 0), HexGrid.Axial(0, 0)))
        assertEquals(1, HexGrid.distance(HexGrid.Axial(0, 0), HexGrid.Axial(1, 0)))
        // 半径 1 内任意两格最远 2
        val far = HexGrid.Axial(1, -1)
        assertEquals(1, HexGrid.distance(HexGrid.Axial(0, 0), far))
    }

    @Test
    fun `key 与轴向互转`() {
        val c = HexGrid.Axial(3, -2)
        assertEquals(c, HexGrid.parseKey(c.key))
        assertEquals(c.key, HexGrid.parseKey("3:-2")!!.key)
        assertEquals(null, HexGrid.parseKey("abc"))
    }

    // ---------- FogService 解锁与分层 ----------

    @Test
    fun `首次到某格解锁其邻域一格 增量解锁不重复`() {
        val engine = FogService(FakeContentRepository(WorldContent(Theme("t"))))
        val origin = HexGrid.Axial(0, 0)

        // 第一次：解锁 origin 的半径 1 邻域（7 个）
        val first = engine.revealAt(origin, emptySet(), PlayerProgress())
        assertEquals(7, first.size)

        // 第二次：把已解锁 set 传入，应无新增
        val dup = engine.revealAt(origin, first.toSet(), PlayerProgress())
        assertTrue(dup.isEmpty())
    }

    @Test
    fun `移动一格后再解锁 新增的是新邻域`() {
        val engine = FogService(FakeContentRepository(WorldContent(Theme("t"))))
        val a = HexGrid.Axial(0, 0)
        val b = HexGrid.Axial(2, 0)
        val atA = engine.revealAt(a, emptySet(), PlayerProgress()).toSet()
        val atB = engine.revealAt(b, atA, PlayerProgress())
        assertTrue(atB.isNotEmpty())
    }

    @Test
    fun `分层显现随探索度推进`() {
        val place = Place("p1", "P", PlaceType.BUILDING, 1.0, 2.0, 10.0,
            history = listOf("h1", "h2"))
        val engine = FogService(FakeContentRepository(WorldContent(Theme("t"))))

        val low = engine.revealableLayers(place, PlayerProgress(exploration = 0.1))
        assertEquals(setOf(FogRevealLayer.PLACE), low)

        val mid = engine.revealableLayers(place, PlayerProgress(exploration = 0.4))
        assertTrue(FogRevealLayer.HISTORY in mid)

        val deep = engine.revealableLayers(place, PlayerProgress(exploration = 0.9))
        assertTrue(FogRevealLayer.HISTORY in deep)
        assertTrue(FogRevealLayer.RUMOR in deep)
        assertTrue(FogRevealLayer.ANOMALY in deep)
    }

    // ---------- Player/Settings 默认 ----------

    @Test
    fun `定位与图层默认值`() {
        val s = PlayerSettings()
        assertEquals(LocationMode.FOREGROUND, s.locationMode)
        assertTrue(s.isLayerVisible(MapLayer.FOG))
        assertTrue(s.isLayerVisible(MapLayer.PLACE))
        assertFalse(s.isLayerVisible(MapLayer.OTHERS)) // 轻社交默认关
        // 迷雾可关
        assertFalse(s.withLayer(MapLayer.FOG, false).isLayerVisible(MapLayer.FOG))
    }
}