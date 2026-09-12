package com.rainingyesterday.data.save

import android.content.Context
import com.rainingyesterday.data.content.ContentJson
import com.rainingyesterday.domain.model.EncyclopediaEntry
import com.rainingyesterday.domain.model.EncyclopediaType
import com.rainingyesterday.domain.model.PlayerSettings
import com.rainingyesterday.domain.model.TrackPoint
import com.rainingyesterday.domain.repository.SaveRepository
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.serializer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 基于 SharedPreferences + JSON 的存档实现。
 * 说明：M4 用轻量方案满足"重启不丢"验收；完整 Room 在 M9 落地并替换本实现（接口不变）。
 */
@Singleton
class PrefsSaveRepository @Inject constructor(
    context: Context,
) : SaveRepository {

    private val prefs = context.getSharedPreferences("player_save", Context.MODE_PRIVATE)
    private val json get() = ContentJson

    // ---- 探索度 ----

    override fun exploration(): Double = prefs.getFloat(KEY_EXPLORATION, 0f).toDouble()
    override fun setExploration(value: Double) {
        prefs.edit().putFloat(KEY_EXPLORATION, value.toFloat()).apply()
    }

    // ---- 图鉴 ----

    override fun encyclopedia(): List<EncyclopediaEntry> {
        val raw = prefs.getString(KEY_ENCY, null) ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(EncyclopediaEntry.serializer()), raw)
        } catch (_: Exception) { emptyList() }
    }

    override fun isEncyclopediaUnlocked(type: EncyclopediaType, refId: String): Boolean =
        encyclopedia().any { it.refType == type && it.refId == refId }

    override fun unlockEncyclopedia(entry: EncyclopediaEntry) {
        val updated = encyclopedia() + entry
        prefs.edit().putString(KEY_ENCY, json.encodeToString(ListSerializer(EncyclopediaEntry.serializer()), updated)).apply()
    }

    // ---- 足迹 ----

    override fun track(): List<TrackPoint> {
        val raw = prefs.getString(KEY_TRACK, null) ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(TrackPoint.serializer()), raw)
        } catch (_: Exception) { emptyList() }
    }

    override fun appendTrackPoint(point: TrackPoint) {
        val updated = track() + point
        prefs.edit().putString(KEY_TRACK, json.encodeToString(ListSerializer(TrackPoint.serializer()), updated)).apply()
    }

    override fun clearTrack() {
        prefs.edit().remove(KEY_TRACK).apply()
    }

    // ---- 设置 ----

    override fun settings(): PlayerSettings {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return PlayerSettings()
        return try {
            json.decodeFromString(PlayerSettings.serializer(), raw)
        } catch (_: Exception) { PlayerSettings() }
    }

    override fun saveSettings(settings: PlayerSettings) {
        prefs.edit().putString(KEY_SETTINGS, json.encodeToString(PlayerSettings.serializer(), settings)).apply()
    }

    private companion object {
        const val KEY_EXPLORATION = "exploration"
        const val KEY_ENCY = "encyclopedia"
        const val KEY_TRACK = "track"
        const val KEY_SETTINGS = "settings"
    }
}