package com.rainingyesterday.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rainingyesterday.data.location.LocationProvider
import com.rainingyesterday.data.location.MockLocationProvider
import com.rainingyesterday.data.location.RealLocationProvider
import com.rainingyesterday.data.location.UserLocation
import com.rainingyesterday.domain.engine.ExplorationService
import com.rainingyesterday.domain.engine.FogService
import com.rainingyesterday.domain.engine.TriggerEngine
import com.rainingyesterday.domain.model.EncyclopediaType
import com.rainingyesterday.domain.model.HexGrid
import com.rainingyesterday.domain.model.LocationMode
import com.rainingyesterday.domain.model.MapLayer
import com.rainingyesterday.domain.model.Place
import com.rainingyesterday.domain.model.PlayerProgress
import com.rainingyesterday.domain.model.PlayerSettings
import com.rainingyesterday.domain.model.TrackPoint
import com.rainingyesterday.domain.repository.ContentRepository
import com.rainingyesterday.domain.repository.SaveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 地图 + 探索 UI 状态：可渲染（对 UI 友好）快照。 */
data class MapUiState(
    val location: UserLocation? = null,
    val locationMode: LocationMode = LocationMode.FOREGROUND,
    val fogVisible: Boolean = true,
    val currentHex: String? = null,
    val exploredKeys: Set<String> = emptySet(),
    val places: List<Place> = emptyList(),
    /** 已发现的地点 id 集合 */
    val discoveredPlaceIds: Set<String> = emptySet(),
    /** 已记录的足迹点 */
    val track: List<TrackPoint> = emptyList(),
    val pendingAutoTrigger: Boolean = false,
    val center: CenterIntent? = null,
)

/** 地图中心意图（把相机位置意图传给 UI）。 */
data class CenterIntent(val lat: Double, val lng: Double)

/** 地图 ViewModel：串起 定位 → 迷雾 → 探索/发现/足迹 → 触发引擎 → 图层开关。 */
@HiltViewModel
class MapViewModel @Inject constructor(
    contentRepository: ContentRepository,
    saveRepository: SaveRepository,
    realLocation: RealLocationProvider,
    mockLocation: MockLocationProvider,
) : ViewModel() {

    private val content = contentRepository
    private val fog = FogService(content)
    private val trigger = TriggerEngine(content)
    private val explore = ExplorationService()
    private val save = saveRepository

    private val settings = MutableStateFlow(save.settings())
    private val explored = MutableStateFlow<Set<String>>(emptySet())
    private val discovered = MutableStateFlow(save.encyclopedia().filter { it.refType == EncyclopediaType.PLACE }.map { it.refId }.toSet())
    private val track = MutableStateFlow(save.track())

    private val real: LocationProvider = realLocation
    private val mock: LocationProvider = mockLocation
    private val activeLocation: (LocationMode) -> LocationProvider =
        { mode -> if (mode == LocationMode.DISABLE) mock else real }

    private val _ui = MutableStateFlow(MapUiState())
    val ui: StateFlow<MapUiState> = _ui.asStateFlow()

    private var currentLocation: UserLocation? = null

    init {
        viewModelScope.launch {
            // 先合成持久/已解锁状态，再与两个定位源合成（combine 最多 5 参，故分层）
            val persistent = combine(settings, explored, discovered, track) { s, ex, disc, trk ->
                Bundle(s, ex, disc, trk)
            }
            combine(persistent, real.location, mock.location) { b, rl, ml ->
                val active = if (b.settings.locationMode == LocationMode.DISABLE) ml else rl
                deriveUiState(b.settings, b.explored, b.discovered, b.track, active)
            }.collect { _ui.value = it }
        }
        startLocation(save.settings().locationMode)
    }

    private class Bundle(
        val settings: PlayerSettings,
        val explored: Set<String>,
        val discovered: Set<String>,
        val track: List<TrackPoint>,
    )

    private fun deriveUiState(
        s: PlayerSettings,
        ex: Set<String>,
        disc: Set<String>,
        trk: List<TrackPoint>,
        loc: UserLocation?,
    ): MapUiState {
        currentLocation = loc
        val posHex = loc?.let { fog.hexAxialAt(it.lat, it.lng, HexGrid.Axial(0, 0), it.lat, it.lng) }
        val player = PlayerProgress(
            exploration = save.exploration(),
            lat = loc?.lat,
            lng = loc?.lng,
        )
        val exploredWithCurrent = if (posHex != null) ex + posHex.key else ex
        val newly = posHex?.let { fog.revealAt(it, exploredWithCurrent, player) } ?: emptyList()
        val merged = exploredWithCurrent + newly
        // 探索/发现/足迹：走进任意地点→[发现]进图鉴；新足迹点写入
        val newDiscoveries = content.places().filter { p ->
            loc != null && explore.isInside(p, loc.lat, loc.lng) && p.id !in disc
        }
        if (newDiscoveries.isNotEmpty()) {
            newDiscoveries.forEach { save.unlockEncyclopedia(com.rainingyesterday.domain.model.EncyclopediaEntry(EncyclopediaType.PLACE, it.id)) }
            discovered.update { it + newDiscoveries.mapNotNull { p -> p.id } }
        }
        // 足迹：与上一点保持最小间隔才记录
        if (loc != null) {
            val last = trk.lastOrNull()
            val fresh = explore.tryAppendTrackPoint(loc.lat, loc.lng, last, minGapM = TRACK_MIN_GAP_M)
            if (fresh != null) {
                save.appendTrackPoint(fresh)
                track.update { it + fresh }
            }
        }

        return MapUiState(
            location = loc,
            locationMode = s.locationMode,
            fogVisible = s.isLayerVisible(MapLayer.FOG),
            currentHex = posHex?.key,
            exploredKeys = merged,
            places = content.places(),
            discoveredPlaceIds = disc + newDiscoveries.mapNotNull { it.id },
            track = trk,
            pendingAutoTrigger = loc != null && newly.isNotEmpty(),
            center = loc?.let { CenterIntent(it.lat, it.lng) },
        )
    }

    fun onMapTap(lat: Double, lng: Double) {
        if (_ui.value.locationMode == LocationMode.DISABLE) {
            (mock as MockLocationProvider).setMock(lat, lng)
        }
    }

    fun setLocationMode(mode: LocationMode) {
        if (mode != LocationMode.DISABLE) (real as LocationProvider).start()
        settings.update { it.copy(locationMode = mode) }
        save.saveSettings(settings.value.copy(locationMode = mode))
    }

    fun toggleFog() {
        settings.update { it.withLayer(MapLayer.FOG, !it.isLayerVisible(MapLayer.FOG)) }
    }

    private fun startLocation(mode: LocationMode) {
        activeLocation(mode).start()
    }

    private companion object {
        const val TRACK_MIN_GAP_M = 15.0
    }
}