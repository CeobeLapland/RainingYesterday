package com.rainingyesterday.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 地点（docs/02_DATA_MODEL.md §2.1） */
@Serializable
data class Place(
    val id: String,
    val name: String,
    val type: PlaceType,
    val lat: Double,
    val lng: Double,
    @SerialName("radius_m")
    val radiusM: Double,
    @SerialName("built_year")
    val builtYear: Int? = null,
    @SerialName("former_name")
    val formerName: String? = null,
    val history: List<String> = emptyList(),
    val resources: List<String> = emptyList(),
    @SerialName("linked_npcs")
    val linkedNpcs: List<String> = emptyList(),
    @SerialName("linked_rumors")
    val linkedRumors: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val ar: ArSpec? = null,
)