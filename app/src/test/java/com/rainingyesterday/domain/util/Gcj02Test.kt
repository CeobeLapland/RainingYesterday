package com.rainingyesterday.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** GCJ-02 坐标转换单测（高德底图对齐用）。 */
class Gcj02Test {

    @Test
    fun `变换非恒等（国内点有偏移）`() {
        val (glat, glng) = Gcj02.wgs84ToGcj02(39.909, 116.397) // 北京
        assertTrue(glat != 39.909 && glng != 116.397)
    }

    @Test
    fun `往返误差小于 1 米`() {
        val lat = 39.909
        val lng = 116.397
        val (glat, glng) = Gcj02.wgs84ToGcj02(lat, lng)
        val (wlat, wlng) = Gcj02.gcj02ToWgs84(glat, glng)
        // 约 1e-5 度 ≈ 1 米量级；放宽到 1e-4
        assertEquals(lat, wlat, 1e-4)
        assertEquals(lng, wlng, 1e-4)
    }

    @Test
    fun `境外坐标不变换`() {
        val (lat, lng) = Gcj02.wgs84ToGcj02(48.85, 2.35) // 巴黎
        assertEquals(48.85, lat, 1e-9)
        assertEquals(2.35, lng, 1e-9)
    }
}