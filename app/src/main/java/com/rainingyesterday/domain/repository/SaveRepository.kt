package com.rainingyesterday.domain.repository

import com.rainingyesterday.domain.model.EncyclopediaEntry
import com.rainingyesterday.domain.model.PlayerSettings
import com.rainingyesterday.domain.model.TrackPoint

/**
 * 玩家存档仓库（docs/02 §2 主"玩家存档动态"；接口在 domain 定义、data 实现）。
 * M4 先落"探索相关"的可持久化存档；完整 Room（M9）在此基础上扩展，本接口为当前里程碑最小集。
 */
interface SaveRepository {

    // ---- 探索度 ----

    fun exploration(): Double
    fun setExploration(value: Double)

    // ---- 图鉴 ----

    fun encyclopedia(): List<EncyclopediaEntry>
    fun isEncyclopediaUnlocked(type: com.rainingyesterday.domain.model.EncyclopediaType, refId: String): Boolean
    fun unlockEncyclopedia(entry: EncyclopediaEntry)

    // ---- 足迹 ----

    fun track(): List<TrackPoint>
    fun appendTrackPoint(point: TrackPoint)
    fun clearTrack()

    // ---- 玩家设置 ----

    fun settings(): PlayerSettings
    fun saveSettings(settings: PlayerSettings)
}