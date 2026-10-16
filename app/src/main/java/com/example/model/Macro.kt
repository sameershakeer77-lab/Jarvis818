package com.example.model

import java.util.UUID

data class Macro(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val trigger: String = "Voice / Prompt Trigger",
    val sourcePrompt: String = "",
    val actions: List<ActionBlock> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val targetResolution: String = "1080x2400 (Vivo Y300 5G)",
    val executionCount: Int = 0,
    val lastExecutedAt: Long? = null
)

data class ExecutionLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val step: Int,
    val message: String,
    val type: LogType = LogType.INFO,
    val coordinateX: Int? = null,
    val coordinateY: Int? = null
)

enum class LogType {
    INFO,
    VISION_AI,
    BLOCK_ADDED,
    EXECUTION_SUCCESS,
    EXECUTION_WARNING,
    EXECUTION_ERROR
}
