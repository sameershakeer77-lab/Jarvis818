package com.example.model

import java.util.UUID

data class ActionBlock(
    val id: String = UUID.randomUUID().toString(),
    val stepNumber: Int = 1,
    val type: ActionBlockType,
    val title: String,
    val description: String,
    val params: Map<String, String> = emptyMap(),
    val status: BlockStatus = BlockStatus.PENDING,
    val executionTimeMs: Long? = null,
    val resultMessage: String? = null
) {
    // Convenient getters for specific action block parameters
    val packageName: String get() = params["packageName"] ?: ""
    val appName: String get() = params["appName"] ?: ""
    val waitSeconds: Int get() = params["seconds"]?.toIntOrNull() ?: 3
    val targetX: Int? get() = params["x"]?.toIntOrNull()
    val targetY: Int? get() = params["y"]?.toIntOrNull()
    val label: String get() = params["label"] ?: ""
    val textToInput: String get() = params["text"] ?: ""
    val pressEnter: Boolean get() = params["pressEnter"]?.toBoolean() ?: true
    val swipeX1: Int? get() = params["x1"]?.toIntOrNull()
    val swipeY1: Int? get() = params["y1"]?.toIntOrNull()
    val swipeX2: Int? get() = params["x2"]?.toIntOrNull()
    val swipeY2: Int? get() = params["y2"]?.toIntOrNull()
    val swipeDurationMs: Long get() = params["durationMs"]?.toLongOrNull() ?: 350L

    fun getParameterSummary(): String {
        return when (type) {
            ActionBlockType.LAUNCH_APP -> "Package: $packageName ($appName)"
            ActionBlockType.WAIT_BEFORE_NEXT -> "Wait: ${waitSeconds}s"
            ActionBlockType.TAKE_SCREENSHOT_AND_ANALYZE -> "Resolution: ${params["resolution"] ?: "1080x2400 (Vivo Y300 5G)"}"
            ActionBlockType.TAP_COORDINATES -> "Coordinates: ($targetX, $targetY) • \"$label\""
            ActionBlockType.INPUT_TEXT -> "Text: \"$textToInput\" (Enter: $pressEnter)"
            ActionBlockType.SWIPE_COORDINATES -> "Swipe: ($swipeX1, $swipeY1) → ($swipeX2, $swipeY2) [${swipeDurationMs}ms]"
        }
    }
}
