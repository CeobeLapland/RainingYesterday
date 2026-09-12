package com.rainingyesterday.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 任务（docs/02_DATA_MODEL.md §2.3）。
 * type == ANOMALY 时必须带 unlock（时空 + 探索度门槛）；allow_empty 允许蹲守落空。
 */
@Serializable
data class Task(
    val id: String,
    val name: String,
    val type: TaskType = TaskType.NORMAL,
    val source: String? = null,
    val unlock: List<Condition> = emptyList(),
    val objectives: List<TaskObjective> = emptyList(),
    @SerialName("allow_empty")
    val allowEmpty: Boolean = false,
    val rewards: TaskRewards? = null,
)

/** 任务目标条目 */
@Serializable
data class TaskObjective(
    val kind: String,
    val place: String? = null,
    val item: String? = null,
    val count: Int = 1,
)

/** 任务奖励：三币 + 解锁项 */
@Serializable
data class TaskRewards(
    val exploration: Int = 0,
    val commemorative: Int = 0,
    val reputation: Int = 0,
    @SerialName("unlock_history")
    val unlockHistory: String? = null,
    @SerialName("unlock_rumor")
    val unlockRumor: String? = null,
)