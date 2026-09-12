package com.rainingyesterday.ui.profile

import androidx.lifecycle.ViewModel
import com.rainingyesterday.domain.model.LocationMode
import com.rainingyesterday.domain.model.MapLayer
import com.rainingyesterday.domain.model.PlayerSettings
import com.rainingyesterday.domain.model.TileSource
import com.rainingyesterday.domain.repository.SaveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** 设置页数据：读写 SaveRepository（与地图共享同一份存档）。 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val save: SaveRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(save.settings())
    val ui: StateFlow<PlayerSettings> = _ui.asStateFlow()

    fun setLocationMode(mode: LocationMode) {
        persist { it.copy(locationMode = mode) }
    }

    fun setTileSource(source: TileSource) {
        persist { it.copy(tileSource = source) }
    }

    fun setTiandituKey(key: String) {
        persist { it.copy(tiandituKey = key) }
    }

    fun setCustomTileUrl(url: String) {
        persist { it.copy(customTileUrl = url) }
    }

    fun toggleFog() {
        persist { it.withLayer(MapLayer.FOG, !it.isLayerVisible(MapLayer.FOG)) }
    }

    private inline fun persist(transform: (PlayerSettings) -> PlayerSettings) {
        val settings = transform(save.settings())
        _ui.value = settings
        save.saveSettings(settings)
    }
}