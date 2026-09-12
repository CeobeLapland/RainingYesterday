package com.rainingyesterday.domain.engine

import com.rainingyesterday.domain.model.FogRevealLayer
import com.rainingyesterday.domain.model.HexGrid
import com.rainingyesterday.domain.model.Place
import com.rainingyesterday.domain.model.PlayerProgress
import com.rainingyesterday.domain.model.PlayerSettings
import com.rainingyesterday.domain.repository.ContentRepository
import kotlin.math.roundToInt

/**
 * 迷雾解锁逻辑（docs/03 §4.2、GDD 7.1）。
 * 玩家真实行走进入某格半径 → 解锁该格（含六邻格，营造平滑揭开），迷雾分层显现：
 * 地点 → 历史 → 传闻 → 异常，由探索度逐层推进。
 *
 * 纯 Kotlin：输入已有格子集(存档) + 玩家位置 + 内容，输出"应新增解锁的格子 + 该格应显示的层"。
 * 每次根据 [PlayerProgress] 推断，便于存档与 UI 只读快照。
 */
class FogService(
    private val content: ContentRepository,
    private val settings: PlayerSettings = PlayerSettings(),
) {

    /** 计算玩家在新格子应额外解锁的格 key 列表（[exploredKeys] 来自存档）。 */
    fun revealAt(
        currentHex: HexGrid.Axial,
        exploredKeys: Set<String>,
        player: PlayerProgress,
    ): List<String> =
        HexGrid.hexesWithinRadius(currentHex, 1)
            .map { it.key }
            .filter { it !in exploredKeys }

    /** 迷雾该格按玩家进度可显现的内容层集合。 */
    fun revealableLayers(place: Place, player: PlayerProgress): Set<FogRevealLayer> {
        val revealed = mutableSetOf(FogRevealLayer.PLACE) // 地点层：走进即显现
        if (player.exploration >= HISTORY_THRESHOLD) revealed += FogRevealLayer.HISTORY
        if (player.exploration >= RUMOR_THRESHOLD) revealed += FogRevealLayer.RUMOR
        if (player.exploration >= ANOMALY_THRESHOLD) revealed += FogRevealLayer.ANOMALY
        return revealed
    }

    /** 由真实经纬度换算玩家所属轴向格子（把玩家位置投到网格上）。 */
    fun hexAxialAt(
        lat: Double,
        lng: Double,
        origin: HexGrid.Axial,
        originLat: Double,
        originLng: Double,
    ): HexGrid.Axial {
        val latPerMeter = 1.0 / 111_320.0
        val lngPerMeter = 1.0 / (111_320.0 * kotlin.math.cos(Math.toRadians(originLat)).coerceAtLeast(0.01))
        val dy = (originLat - lat) / latPerMeter
        val dx = (lng - originLng) / lngPerMeter
        val step = settings.hexSideMeters
        return HexGrid.Axial(
            (origin.q + (dx / (step * THREE_HALF)).roundToInt()),
            (origin.r + (-dy / (step * SQRT3_HALF)).roundToInt()),
        )
    }

    private companion object {
        const val HISTORY_THRESHOLD = 0.3
        const val RUMOR_THRESHOLD = 0.6
        const val ANOMALY_THRESHOLD = 0.8
        const val THREE_HALF = 3.0 / 2.0
        const val SQRT3_HALF = 1.7320508075688772 / 2.0
    }
}