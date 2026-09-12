package com.rainingyesterday.domain.model

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 六边形迷雾网格（docs/02 FogCell 的"形状决定者"）。
 * 用轴向坐标（q, r）承载六边形算法（近邻、半径围簇、坐标转换），
 * 存储时用稳定字符串 key（"q:r"）作为 [FogCell.hexId]。
 *
 * 本类只做几何/拓扑计算，不持有任何世界状态，纯 Kotlin、可单测。
 * 迷雾谜题：docs/01 §3、02 FogCell。半径/格大小等常量可配置，见 FogService。
 */
object HexGrid {

    /** 轴向坐标，公开给 2D 绘制转换用。 */
    data class Axial(val q: Int, val r: Int) {
        val key: String get() = "$q:$r"
        override fun toString(): String = key
    }

    /**
     * 以六边形大小（中心到顶点的"边长"）为题。
     * 生成以 origin（可传入玩家锚点）为中心、半径 [radiusInCells] 内的所有六边形坐标。
     */
    fun hexesWithinRadius(origin: Axial, radiusInCells: Int): List<Axial> {
        if (radiusInCells < 0) return emptyList()
        val result = mutableListOf<Axial>()
        repeat(radiusInCells * 2 + 1) { _dq ->
            val dq = _dq - radiusInCells
            val dqAbs = abs(dq)
            repeat(radiusInCells * 2 + 1 - dqAbs) { _dr ->
                val dr = _dr - radiusInCells + (dqAbs + dq) / 2
                result += Axial(origin.q + dq, origin.r + dr)
            }
        }
        return result
    }

    /** 轴向相邻六边形（六个方向）。 */
    fun neighbors(c: Axial): List<Axial> = DIRECTIONS.map { Axial(c.q + it.q, c.r + it.r) }

    /** 两个六边形间的轴向距离（移动步数）。 */
    fun distance(a: Axial, b: Axial): Int =
        (abs(a.q - b.q) + abs(a.q + a.r - b.q - b.r) + abs(a.r - b.r)) / 2

    /** 网格六边形半径（外接圆半径），由边长 side 推算。用于将轴向坐标换算成像素。 */
    fun hexRadiusPx(side: Double): Double = side

    /**
     * 轴向坐标 → offset 坐标（row offset，奇行偏右），用于贴合屏幕逐格绘制。
     * osmdroid 兜底绘制时用；MVP 若只画"当前可见区域内格"也可直接遍历 Axial。
     */
    fun toOffset(c: Axial): List<Int> {
        val col = c.q + (c.r - (c.r and 1)) / 2
        return listOf(col, c.r)
    }

    /** 坐标系中一个格子的中心像素位置（轴向 → 平铺像素），供 Canvas 绘制。 */
    fun axialToPoint(c: Axial, gridSide: Double): Pair<Double, Double> {
        val size = hexRadiusPx(gridSide)
        val x = size * (c.q * 3.0 / 2.0)
        val y = size * (sqrt(3.0) * (c.r + c.q / 2.0))
        return x to y
    }

    /** 解析 "q:r" key 回轴向。非法输入返回 null。 */
    fun parseKey(key: String): Axial? {
        val p = key.split(":")
        if (p.size != 2) return null
        val q = p[0].toIntOrNull() ?: return null
        val r = p[1].toIntOrNull() ?: return null
        return Axial(q, r)
    }

    /**
     * 轴向坐标 → 世界米偏移（相对世界原点，pointy-top 平铺）。
     * 六边形中心距格间距 = hexSide * sqrt(3)，行距 = hexSide * 1.5。
     */
    fun axialToWorldMeters(c: Axial, hexSide: Double): Pair<Double, Double> {
        val x = hexSide * (c.q * 1.5 + c.r * 0.75)
        val y = hexSide * (c.r * sqrt(3.0) / 2.0)
        return x to y
    }

    /**
     * 世界米偏移 → 最近轴向坐标（[axialToWorldMeters] 的逆，供外部回查格子归属）。
     */
    fun worldMetersToAxial(xM: Double, yM: Double, hexSide: Double): Axial {
        // pointy-top 反解（红康 formula region），取最近格子
        val r = (yM / (hexSide * sqrt(3.0) / 2.0)).roundToInt()
        val q = ((xM / hexSide) - 0.75 * r).roundToInt()
        return Axial(q, r)
    }

    /**
     * 世界经纬度 → 最近轴向坐标（以世界原点 + 格大小为锚，反向求格子归属）。
     * [latLngToAxial] 是 [axialToLatLng] 的逆，两者一致 → 格子钉死在地球坐标。
     */
    fun latLngToAxial(
        lat: Double, lng: Double,
        worldOriginLat: Double, worldOriginLng: Double,
        hexSideMeters: Double,
    ): Axial {
        val latPerMeter = 1.0 / 111_320.0
        val lngPerMeter = 1.0 / (111_320.0 * kotlin.math.cos(Math.toRadians(worldOriginLat)).coerceAtLeast(0.01))
        val xM = (lng - worldOriginLng) / lngPerMeter
        val yM = -(lat - worldOriginLat) / latPerMeter
        return worldMetersToAxial(xM, yM, hexSideMeters)
    }

    /** 轴向坐标 → 世界经纬度（[latLngToAxial] 的逆）。格子中心经纬度。 */
    fun axialToLatLng(
        c: Axial,
        worldOriginLat: Double, worldOriginLng: Double,
        hexSideMeters: Double,
    ): Pair<Double, Double> {
        val (xM, yM) = axialToWorldMeters(c, hexSideMeters)
        val latPerMeter = 1.0 / 111_320.0
        val lngPerMeter = 1.0 / (111_320.0 * kotlin.math.cos(Math.toRadians(worldOriginLat)).coerceAtLeast(0.01))
        val lat = worldOriginLat - yM * latPerMeter
        val lng = worldOriginLng + xM * lngPerMeter
        return lat to lng
    }

    /**
     * 屏幕像素 → 世界米（相对屏幕中心），供 UI 把点击像素转成世界坐标，再回查格子。
     */
    fun pixelToWorldMeters(pxX: Double, pxY: Double, metersPerPixel: Double): Pair<Double, Double> =
        (pxX * metersPerPixel) to (pxY * metersPerPixel)

    private val DIRECTIONS = listOf(
        Axial(1, 0), Axial(1, -1), Axial(0, -1),
        Axial(-1, 0), Axial(-1, 1), Axial(0, 1),
    )

    /** 从像素点反推轴向坐标（用于点击/落子定位），MVP 辅助。 */
    fun pointToAxial(x: Double, y: Double, gridSide: Double): Axial {
        val size = hexRadiusPx(gridSide)
        val q = ((x * sqrt(3.0) / 3.0 - y / 3.0) / size).roundToInt()
        val r = (y * 2.0 / 3.0 / size).roundToInt()
        return Axial(q, r)
    }
}