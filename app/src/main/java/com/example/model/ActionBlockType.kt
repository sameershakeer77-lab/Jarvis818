package com.example.model

enum class ActionBlockType(val displayName: String, val category: String) {
    LAUNCH_APP("Launch App", "Application"),
    WAIT_BEFORE_NEXT("Wait Before Next", "Timing"),
    TAKE_SCREENSHOT_AND_ANALYZE("Take Screenshot & Analyze", "Vision AI"),
    TAP_COORDINATES("Tap Coordinates", "Touch Interaction"),
    INPUT_TEXT("Input Text", "Text & Input"),
    SWIPE_COORDINATES("Swipe Coordinates", "Touch Interaction");

    companion object {
        fun fromString(name: String): ActionBlockType {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: TAP_COORDINATES
        }
    }
}

enum class BlockStatus {
    PENDING,
    EXECUTING,
    COMPLETED,
    FAILED,
    SKIPPED
}
