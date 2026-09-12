package com.rainingyesterday.domain.model

/**
 * 地图图层（docs/01 分层、GDD 过去/现在与表层/深层切换的载体）。
 * 每个图层可独立开关；本质是一批"显示规则"，对应玩家的显示偏好（迷雾可关等）。
 */
enum class MapLayer(val defaultVisible: Boolean) {
    BASE(true),          // 底图（OSM 瓦片），恒开
    FOG(true),           // 迷雾（可关）
    PLACE(true),         // 地点标记（主动可见）
    TRACE(true),         // 足迹路线
    NPC(true),           // NPC / 事件 / 传闻标记（主动可见）
    OTHERS(false),       // 他人足迹 / 共享记忆（轻社交，默认关）
}