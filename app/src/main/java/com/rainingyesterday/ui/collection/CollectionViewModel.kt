package com.rainingyesterday.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rainingyesterday.domain.model.EncyclopediaType
import com.rainingyesterday.domain.model.Place
import com.rainingyesterday.domain.repository.ContentRepository
import com.rainingyesterday.domain.repository.SaveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 图鉴 UI 状态：所有地点 + 已发现集合 + 进度。 */
data class CollectionUiState(
    val places: List<Place> = emptyList(),
    val discoveredPlaceIds: Set<String> = emptySet(),
) {
    val discoveredCount: Int get() = discoveredPlaceIds.size
    val totalPlaces: Int get() = places.size
}

/** 世界图鉴（GDD 7.14）：读取地点与已发现集合，展示收集进度。 */
@HiltViewModel
class CollectionViewModel @Inject constructor(
    contentRepository: ContentRepository,
    saveRepository: SaveRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(CollectionUiState(places = contentRepository.places()))
    val ui: StateFlow<CollectionUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val discovered = saveRepository.encyclopedia()
                .filter { it.refType == EncyclopediaType.PLACE }
                .map { it.refId }.toSet()
            _ui.value = _ui.value.copy(discoveredPlaceIds = discovered)
        }
    }
}