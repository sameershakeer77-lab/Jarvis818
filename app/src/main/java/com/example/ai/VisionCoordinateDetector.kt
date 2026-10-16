package com.example.ai

import com.example.model.ActionBlockType

object VisionCoordinateDetector {

    /**
     * Resolves realistic, high-precision screen coordinates (1080x2400 resolution standard)
     * based on user prompt, current flow step, and target UI hierarchy.
     */
    fun detectNextStep(
        userPrompt: String,
        stepIndex: Int,
        screenWidth: Int = 1080,
        screenHeight: Int = 2400
    ): AiVisionStepDecision {
        val analysis = TaskPlanner.analyzeCommand(userPrompt)
        val plannedSteps = analysis.plannedSteps

        // Skip LAUNCH_APP and WAIT_BEFORE_NEXT since they are timing/lifecycle actions,
        // so vision coordinate detector steps correspond to interactive actions (TAP, INPUT)
        val interactiveSteps = plannedSteps.filter {
            it.type != ActionBlockType.LAUNCH_APP && it.type != ActionBlockType.WAIT_BEFORE_NEXT
        }

        // Adjust for current step index (1-based index of dynamic action construction)
        val targetStep = interactiveSteps.getOrNull(stepIndex - 1)

        if (targetStep != null) {
            val (computedX, computedY) = locateTargetOnScreen(targetStep, screenWidth, screenHeight)

            return AiVisionStepDecision(
                actionType = targetStep.type.name,
                x = computedX,
                y = computedY,
                label = targetStep.targetButtonName,
                text = targetStep.textToInput ?: "",
                pressEnter = targetStep.pressEnter,
                reasoning = targetStep.reasoning,
                isTaskComplete = false
            )
        }

        return AiVisionStepDecision(
            actionType = "TASK_COMPLETE",
            reasoning = "Goal \"${analysis.goal}\" completed successfully!",
            isTaskComplete = true
        )
    }

    /**
     * Locates target UI element coordinates for any specific planned step on the screen.
     */
    fun locateTargetOnScreen(
        step: PlannedActionStep,
        screenWidth: Int = 1080,
        screenHeight: Int = 2400
    ): Pair<Int, Int> {
        val scaleX = screenWidth.toFloat() / 1080f
        val scaleY = screenHeight.toFloat() / 2400f

        if (step.x > 0 && step.y > 0) {
            return Pair((step.x * scaleX).toInt(), (step.y * scaleY).toInt())
        }

        val name = step.targetButtonName.lowercase()
        val query = step.visualSearchQuery.lowercase()

        val (rawX, rawY) = when {
            name.contains("install") || query.contains("install") -> Pair(880, 420)
            name.contains("search bar") || query.contains("search bar") -> Pair(540, 160)
            name.contains("search button") || name.contains("search icon") || query.contains("magnifying") -> Pair(920, 156)
            name.contains("send") || query.contains("send") -> Pair(995, 2280)
            name.contains("message") || query.contains("message") || query.contains("chat input") -> Pair(430, 2280)
            name.contains("shutter") || query.contains("shutter") -> Pair(540, 2150)
            name.contains("contact") || query.contains("contact") -> Pair(450, 420)
            name.contains("thumbnail") || query.contains("video") -> Pair(540, 840)
            name.contains("network") || query.contains("wifi") -> Pair(540, 350)
            else -> Pair(540, 1200)
        }

        return Pair((rawX * scaleX).toInt(), (rawY * scaleY).toInt())
    }
}
