package com.rainingyesterday.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 配方（docs/02_DATA_MODEL.md §2.5）：简化加工产线 */
@Serializable
data class Recipe(
    val id: String,
    val name: String,
    val machine: String,
    val inputs: Map<String, Int> = emptyMap(),
    val output: RecipeOutput,
    @SerialName("duration_hours")
    val durationHours: Int = 1,
    val condition: List<Condition> = emptyList(),
)

@Serializable
data class RecipeOutput(
    val item: String,
    val count: Int = 1,
)