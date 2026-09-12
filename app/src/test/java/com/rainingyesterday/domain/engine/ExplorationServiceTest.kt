package com.rainingyesterday.domain.engine

import com.rainingyesterday.domain.model.EncyclopediaEntry
import com.rainingyesterday.domain.model.EncyclopediaType
import com.rainingyesterday.domain.model.Place
import com.rainingyesterday.domain.model.PlaceType
import com.rainingyesterday.domain.model.TrackPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** M4 探索核心单测：走进发现、足迹去噪、幂等、周统计。 */
class ExplorationServiceTest {

    private val svc = ExplorationService()
    private val lake = Place("lake", "湖边", PlaceType.NATURE, 31.2, 121.4, 60.0)

    @Test
    fun `走进地点判定`() {
        assertTrue(svc.isInside(lake, 31.2001, 121.4001)) // 很近，在 60m 半径内
        val far = svc.isInside(lake, 31.3, 121.5)
        assertEquals(false, far)
    }

    @Test
    fun `足迹去噪 过近不记录`() {
        val last = TrackPoint(31.2, 121.4, 1000)
        // 几乎同一点 -> 去噪
        val tooClose = svc.tryAppendTrackPoint(31.2001, 121.4001, last, minGapM = 20.0)
        assertNull(tooClose)
        // 明显位移 -> 记录
        val moved = svc.tryAppendTrackPoint(31.21, 121.41, last, minGapM = 20.0)
        assertNotNull(moved)
    }

    @Test
    fun `首点总是记录`() {
        val first = svc.tryAppendTrackPoint(31.2, 121.4, null, minGapM = 20.0)
        assertNotNull(first)
    }

    @Test
    fun `图鉴发现幂等`() {
        val already = mapOf("lake" to EncyclopediaEntry(EncyclopediaType.PLACE, "lake"))
        assertNull(svc.discoverPlace(lake, already))
        val fresh = svc.discoverPlace(lake, emptyMap())
        assertEquals("lake", fresh!!.refId)
        assertEquals(EncyclopediaType.PLACE, fresh.refType)
    }

    @Test
    fun `周足迹统计`() {
        val now = 2_000_000_000_000L
        val track = listOf(
            TrackPoint(31.2, 121.4, now - 1000),         // 1 秒前
            TrackPoint(31.2, 121.4, now - 3 * 24 * 3600_000L), // 3 天前
            TrackPoint(31.2, 121.4, now - 8 * 24 * 3600_000L), // 8 天前（超一周）
        )
        assertEquals(2, svc.weekStepCount(track, now))
    }
}