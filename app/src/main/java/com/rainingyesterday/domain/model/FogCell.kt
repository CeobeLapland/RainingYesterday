package com.rainingyesterday.domain.model

/**
 * 迷雾格（docs/02 FogCell 的领域形态）：一个六边形格子的解锁状态。
 * 真实存档（Room）在 M9 落地，本类作为领域引擎的内存视图。
 * hexId 由 [HexGrid.Axial.key] 产生。
 */
data class FogCell(
    val hexId: String,
    var explored: Boolean = false,
)

/** 迷雾分层显现层级（docs/01 §2.2 探索四层）：地点→历史→传闻→异常 */
enum class FogRevealLayer { PLACE, HISTORY, RUMOR, ANOMALY }