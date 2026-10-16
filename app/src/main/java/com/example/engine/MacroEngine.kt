package com.example.engine

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import com.example.ai.GeminiVisionClient
import com.example.ai.PlannedActionStep
import com.example.ai.TaskAnalysis
import com.example.ai.TaskPlanner
import com.example.ai.VisionCoordinateDetector
import com.example.data.MacroRepository
import com.example.data.SettingsManager
import com.example.model.ActionBlock
import com.example.model.ActionBlockType
import com.example.model.BlockStatus
import com.example.model.ExecutionLog
import com.example.model.LogType
import com.example.model.Macro
import com.example.service.MacroExecutorService
import com.example.util.DeviceDisplayUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class EngineState {
    IDLE,
    BUILDING_AND_EXECUTING,
    PAUSED,
    REPLAYING,
    COMPLETED,
    ERROR
}

data class TargetCrosshair(
    val x: Int,
    val y: Int,
    val label: String,
    val step: Int,
    val timestamp: Long = System.currentTimeMillis()
)

class MacroEngine(
    private val context: Context,
    private val repository: MacroRepository,
    private val settingsManager: SettingsManager,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "MacroEngine"
    }

    private val visionClient = GeminiVisionClient()
    private var engineJob: Job? = null

    // State flows for Compose UI
    private val _engineState = MutableStateFlow(EngineState.IDLE)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    private val _activeMacro = MutableStateFlow<Macro?>(null)
    val activeMacro: StateFlow<Macro?> = _activeMacro.asStateFlow()

    private val _taskAnalysis = MutableStateFlow<TaskAnalysis?>(null)
    val taskAnalysis: StateFlow<TaskAnalysis?> = _taskAnalysis.asStateFlow()

    private val _currentCrosshair = MutableStateFlow<TargetCrosshair?>(null)
    val currentCrosshair: StateFlow<TargetCrosshair?> = _currentCrosshair.asStateFlow()

    private val _currentScreenBitmap = MutableStateFlow<Bitmap?>(null)
    val currentScreenBitmap: StateFlow<Bitmap?> = _currentScreenBitmap.asStateFlow()

    private val _screenStageName = MutableStateFlow("Device Standby")
    val screenStageName: StateFlow<String> = _screenStageName.asStateFlow()

    private val _logs = MutableStateFlow<List<ExecutionLog>>(emptyList())
    val logs: StateFlow<List<ExecutionLog>> = _logs.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private val _isDeviceExecuting = MutableStateFlow(false)
    val isDeviceExecuting: StateFlow<Boolean> = _isDeviceExecuting.asStateFlow()

    init {
        _currentScreenBitmap.value = generateMockScreen("Standby", null, 1080, 2400)
    }

    /**
     * Entry point for executing any user command:
     * 1. Searches and plans the procedural action steps for the given task
     * 2. Opens target app
     * 3. For each step: Takes a screenshot, searches target element to obtain coordinates,
     *    clicks/types on device, updates status, and repeats until task is complete.
     */
    fun startBuildAndExecute(prompt: String) {
        if (prompt.isBlank()) return
        engineJob?.cancel()

        engineJob = coroutineScope.launch(Dispatchers.Default) {
            val service = MacroExecutorService.instance
            val isRealDevice = service != null && !settingsManager.simulationMode.value
            _isDeviceExecuting.value = isRealDevice

            val deviceSize = DeviceDisplayUtils.getRealDisplaySize(context)

            try {
                _engineState.value = EngineState.BUILDING_AND_EXECUTING
                _logs.value = emptyList()
                _currentStepIndex.value = 1

                // 1. AI Task Search & Procedural Step Generation
                addLog(1, "🔍 AI Searching procedural steps for: \"$prompt\"...", LogType.VISION_AI)
                val analysis = TaskPlanner.analyzeCommand(prompt, context)
                _taskAnalysis.value = analysis

                val packageName = analysis.targetAppPackage
                val appName = analysis.targetAppName
                val macroTitle = "$appName: ${analysis.goal.take(36)}"

                val modeDescription = if (isRealDevice) {
                    "ACTIVE PHYSICAL DEVICE (${deviceSize.width}x${deviceSize.height})"
                } else {
                    "INTERACTIVE SIMULATOR PREVIEW"
                }

                addLog(1, "Starting AIMacroDroid on $modeDescription", LogType.INFO)
                addLog(1, "🎯 Goal: ${analysis.goal}", LogType.INFO)
                addLog(1, "📱 Target App: $appName ($packageName)", LogType.INFO)
                analysis.plannedStepsDescription.forEachIndexed { i, s ->
                    addLog(1, "Step #${i + 1}: $s", LogType.INFO)
                }

                // Initialize Macro
                val initialMacro = Macro(
                    id = UUID.randomUUID().toString(),
                    title = macroTitle,
                    trigger = "Voice / AI Prompt: $prompt",
                    sourcePrompt = prompt,
                    targetResolution = "${deviceSize.width}x${deviceSize.height}",
                    actions = emptyList(),
                    isCompleted = false
                )
                _activeMacro.value = initialMacro
                addLog(1, "STEP 1: Initialized new MacroDroid Macro \"$macroTitle\"", LogType.INFO)

                // ACTION BLOCK 1: Launch Target App
                val block1 = ActionBlock(
                    id = UUID.randomUUID().toString(),
                    stepNumber = 1,
                    type = ActionBlockType.LAUNCH_APP,
                    title = "Launch $appName",
                    description = "Launch application package $packageName",
                    params = mapOf("packageName" to packageName, "appName" to appName),
                    status = BlockStatus.EXECUTING
                )
                appendActionBlock(block1)
                addLog(1, "Appended Action Block #1: Launch App [$appName ($packageName)]", LogType.BLOCK_ADDED)

                if (isRealDevice) {
                    service?.showFloatingOverlay("Opening $appName...") { stopExecution() }
                    val launched = service?.launchTargetApp(packageName) ?: false
                    if (!launched) {
                        try {
                            val rawAppName = analysis.parameters["appName"] ?: analysis.parameters["game"] ?: analysis.parameters["query"] ?: "apps"
                            val encodedQuery = java.net.URLEncoder.encode(rawAppName, "UTF-8")
                            val fallback = context.packageManager.getLaunchIntentForPackage(packageName)
                                ?: if (packageName.contains("vending")) {
                                    Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://search?q=$encodedQuery"))
                                } else if (packageName.contains("whatsapp")) {
                                    Intent(Intent.ACTION_MAIN).setPackage("com.whatsapp")
                                } else if (packageName.contains("youtube")) {
                                    Intent(Intent.ACTION_VIEW, android.net.Uri.parse("vnd.youtube:"))
                                } else null
                            fallback?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

                            if (fallback != null && fallback.resolveActivity(context.packageManager) != null) {
                                context.startActivity(fallback)
                                addLog(1, "Launched target via system intent: $packageName", LogType.INFO)
                            } else if (packageName.contains("vending")) {
                                val webIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/search?q=$encodedQuery&c=apps")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                if (webIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(webIntent)
                                    addLog(1, "Opened Play Store in browser", LogType.INFO)
                                } else {
                                    addLog(1, "Google Play Store app not detected on device; using simulated UI", LogType.INFO)
                                }
                            } else {
                                addLog(1, "$appName not installed locally; using simulated UI", LogType.INFO)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Fallback launch note: ${e.message}")
                            addLog(1, "Proceeding with screen simulation for $appName", LogType.INFO)
                        }
                    } else {
                        addLog(1, "Physically launched $appName on mobile screen", LogType.EXECUTION_SUCCESS)
                    }
                }

                markBlockStatus(block1.id, BlockStatus.COMPLETED, 120L, "Launched $appName")
                _screenStageName.value = "$appName Home"
                _currentScreenBitmap.value = generateMockScreen(appName, "HOME", 1080, 2400, analysis.parameters["appName"] ?: analysis.parameters["query"] ?: "")

                // ACTION BLOCK 2: Wait for UI to render
                addLog(2, "STEP 2: Waiting for $appName UI and hierarchy to render...", LogType.INFO)
                val waitBlock = ActionBlock(
                    id = UUID.randomUUID().toString(),
                    stepNumber = 2,
                    type = ActionBlockType.WAIT_BEFORE_NEXT,
                    title = "Wait 2s for $appName Load",
                    description = "Allow UI elements and network feed to render",
                    params = mapOf("seconds" to "2"),
                    status = BlockStatus.EXECUTING
                )
                appendActionBlock(waitBlock)
                delay(1800L)
                markBlockStatus(waitBlock.id, BlockStatus.COMPLETED, 1800L, "UI ready")

                // Step-by-Step Closed-Loop Execution:
                // For each planned interactive step:
                // 1. Take a screenshot
                // 2. Search screenshot with step target to find coordinates
                // 3. Click coordinates or input text
                // 4. Update screen and repeat until task complete!
                val interactivePlannedSteps = analysis.plannedSteps.filter {
                    it.type != ActionBlockType.LAUNCH_APP && it.stepIndex > 2
                }

                val aiApiKey = settingsManager.geminiApiKey.value
                var stepCounter = 1

                for (plannedStep in interactivePlannedSteps) {
                    val currentStepNumber = (_activeMacro.value?.actions?.size ?: 0) + 1
                    _currentStepIndex.value = currentStepNumber

                    // 1. Take screenshot
                    addLog(currentStepNumber, "📸 Step $currentStepNumber: Taking screenshot for \"${plannedStep.title}\"...", LogType.VISION_AI)
                    val currentScreenshot = if (isRealDevice) {
                        service?.captureScreenSuspend() ?: _currentScreenBitmap.value
                    } else {
                        _currentScreenBitmap.value
                    }
                    if (currentScreenshot != null) {
                        _currentScreenBitmap.value = currentScreenshot
                    }

                    // 2. Search screenshot with step target to find coordinates
                    addLog(currentStepNumber, "🔎 Searching screenshot for: \"${plannedStep.targetButtonName}\" [${plannedStep.visualSearchQuery}]...", LogType.VISION_AI)
                    val (foundX, foundY) = visionClient.locateTargetCoordinates(
                        apiKey = aiApiKey,
                        screenshotBitmap = currentScreenshot,
                        step = plannedStep,
                        screenWidth = deviceSize.width,
                        screenHeight = deviceSize.height
                    )
                    addLog(currentStepNumber, "🎯 Located target \"${plannedStep.targetButtonName}\" at ($foundX, $foundY)", LogType.VISION_AI, foundX, foundY)

                    // 3. Show visual crosshair on screen preview and floating overlay
                    _currentCrosshair.value = TargetCrosshair(
                        x = foundX,
                        y = foundY,
                        label = plannedStep.targetButtonName,
                        step = currentStepNumber
                    )

                    // 4. Execute action
                    when (plannedStep.type) {
                        ActionBlockType.TAP_COORDINATES -> {
                            val (realX, realY) = DeviceDisplayUtils.mapCoordinatesToDevice(
                                aiX = foundX,
                                aiY = foundY,
                                referenceWidth = 1080,
                                referenceHeight = 2400,
                                deviceSize = deviceSize
                            )

                            val tapBlock = ActionBlock(
                                id = UUID.randomUUID().toString(),
                                stepNumber = currentStepNumber,
                                type = ActionBlockType.TAP_COORDINATES,
                                title = plannedStep.title,
                                description = plannedStep.description,
                                params = mapOf("x" to foundX.toString(), "y" to foundY.toString(), "button" to plannedStep.targetButtonName),
                                status = BlockStatus.EXECUTING
                            )
                            appendActionBlock(tapBlock)
                            addLog(currentStepNumber, "Appended Action Block #$currentStepNumber: ${tapBlock.title}", LogType.BLOCK_ADDED)

                            if (isRealDevice) {
                                service?.showFloatingOverlay("Step $currentStepNumber: Clicking ${plannedStep.targetButtonName}...") { stopExecution() }

                                // Search active window for matching accessibility node first, fallback to tap coordinates
                                val nodeBounds = service?.findAndClickNodeByKeywords(plannedStep.searchKeywords)
                                val tapSuccess = if (nodeBounds != null) {
                                    addLog(currentStepNumber, "Found matching node on device at (${nodeBounds.first}, ${nodeBounds.second}) and clicked", LogType.EXECUTION_SUCCESS)
                                    true
                                } else {
                                    service?.dispatchTapSuspend(realX, realY) ?: false
                                }

                                addLog(currentStepNumber, "Physical click executed: $tapSuccess", if (tapSuccess) LogType.EXECUTION_SUCCESS else LogType.EXECUTION_WARNING)
                            }

                            delay(settingsManager.stepDelayMs.value)
                            markBlockStatus(tapBlock.id, BlockStatus.COMPLETED, 95L, "Clicked ${plannedStep.targetButtonName}")

                            updateMockScreenForStep(appName, stepCounter, analysis)
                        }

                        ActionBlockType.INPUT_TEXT -> {
                            val textToInput = plannedStep.textToInput ?: ""
                            val inputBlock = ActionBlock(
                                id = UUID.randomUUID().toString(),
                                stepNumber = currentStepNumber,
                                type = ActionBlockType.INPUT_TEXT,
                                title = plannedStep.title,
                                description = plannedStep.description,
                                params = mapOf("text" to textToInput, "pressEnter" to plannedStep.pressEnter.toString()),
                                status = BlockStatus.EXECUTING
                            )
                            appendActionBlock(inputBlock)
                            addLog(currentStepNumber, "Appended Action Block #$currentStepNumber: ${inputBlock.title}", LogType.BLOCK_ADDED)

                            if (isRealDevice) {
                                service?.showFloatingOverlay("Step $currentStepNumber: Typing \"$textToInput\"...") { stopExecution() }
                                val inputSuccess = service?.inputTextOnDevice(textToInput, plannedStep.pressEnter) ?: false
                                addLog(currentStepNumber, "Real device text injection: $inputSuccess", if (inputSuccess) LogType.EXECUTION_SUCCESS else LogType.EXECUTION_WARNING)
                            }

                            delay(settingsManager.stepDelayMs.value)
                            markBlockStatus(inputBlock.id, BlockStatus.COMPLETED, 120L, "Typed \"$textToInput\"")

                            updateMockScreenForStep(appName, stepCounter, analysis)
                        }

                        ActionBlockType.WAIT_BEFORE_NEXT -> {
                            val waitBlockInner = ActionBlock(
                                id = UUID.randomUUID().toString(),
                                stepNumber = currentStepNumber,
                                type = ActionBlockType.WAIT_BEFORE_NEXT,
                                title = plannedStep.title,
                                description = plannedStep.description,
                                params = mapOf("seconds" to "2"),
                                status = BlockStatus.EXECUTING
                            )
                            appendActionBlock(waitBlockInner)
                            delay(1500L)
                            markBlockStatus(waitBlockInner.id, BlockStatus.COMPLETED, 1500L, "Wait complete")
                            updateMockScreenForStep(appName, stepCounter, analysis)
                        }

                        else -> {
                            delay(400L)
                        }
                    }

                    stepCounter++
                }

                // Finalize and save Macro
                _activeMacro.value?.let { current ->
                    val finishedMacro = current.copy(
                        isCompleted = true,
                        executionCount = current.executionCount + 1,
                        lastExecutedAt = System.currentTimeMillis()
                    )
                    _activeMacro.value = finishedMacro
                    repository.saveMacro(finishedMacro)
                }

                addLog(interactivePlannedSteps.size + 2, "🎉 Macro construction & device execution completed! Saved to Tab 2.", LogType.EXECUTION_SUCCESS)
                _engineState.value = EngineState.COMPLETED
                service?.showFloatingOverlay("Task Complete! 🎉") { stopExecution() }
                delay(2000L)
                service?.hideFloatingOverlay()
            } catch (e: Exception) {
                Log.e(TAG, "Error during Macro construction/execution", e)
                addLog(_currentStepIndex.value, "Execution error on device: ${e.message}", LogType.EXECUTION_ERROR)
                _engineState.value = EngineState.ERROR
                service?.hideFloatingOverlay()
            } finally {
                _isDeviceExecuting.value = false
            }
        }
    }

    private fun updateMockScreenForStep(appName: String, stepIndex: Int, analysis: TaskAnalysis) {
        val isPlayStore = appName.contains("Play Store", ignoreCase = true) || appName.contains("vending", ignoreCase = true) || analysis.actionCategory == "APP_DOWNLOAD"
        val query = analysis.parameters["appName"] ?: analysis.parameters["game"] ?: analysis.parameters["query"] ?: "Candy Crush"

        if (isPlayStore) {
            when (stepIndex) {
                1 -> {
                    _screenStageName.value = "Play Store Search Active"
                    _currentScreenBitmap.value = generateMockScreen(appName, "SEARCH_FOCUSED", 1080, 2400, query)
                }
                2, 3 -> {
                    _screenStageName.value = "Play Store Results: $query"
                    _currentScreenBitmap.value = generateMockScreen(appName, "RESULTS", 1080, 2400, query)
                }
                4 -> {
                    _screenStageName.value = "Downloading $query..."
                    _currentScreenBitmap.value = generateMockScreen(appName, "DOWNLOADING", 1080, 2400, query)
                }
                else -> {
                    _screenStageName.value = "$query Installed"
                    _currentScreenBitmap.value = generateMockScreen(appName, "INSTALLED", 1080, 2400, query)
                }
            }
        } else if (appName.equals("WhatsApp", ignoreCase = true)) {
            val contact = analysis.parameters["contact"] ?: "shakeer"
            val message = analysis.parameters["message"] ?: "hi"
            when (stepIndex) {
                1 -> {
                    _screenStageName.value = "WhatsApp Search Active"
                    _currentScreenBitmap.value = generateMockScreen(appName, "SEARCH_FOCUSED", 1080, 2400, "")
                }
                2 -> {
                    _screenStageName.value = "Filtered Contact: $contact"
                    _currentScreenBitmap.value = generateMockScreen(appName, "SEARCH_FOCUSED", 1080, 2400, contact)
                }
                3, 4 -> {
                    _screenStageName.value = "Chat with $contact"
                    _currentScreenBitmap.value = generateMockScreen(appName, "CHAT_OPEN", 1080, 2400, "$contact:$message")
                }
                else -> {
                    _screenStageName.value = "Message Sent to $contact"
                    _currentScreenBitmap.value = generateMockScreen(appName, "MESSAGE_SENT", 1080, 2400, "$contact:$message")
                }
            }
        } else if (appName.equals("YouTube", ignoreCase = true)) {
            when (stepIndex) {
                1 -> {
                    _screenStageName.value = "YouTube Search Active"
                    _currentScreenBitmap.value = generateMockScreen(appName, "SEARCH_FOCUSED", 1080, 2400, query)
                }
                2 -> {
                    _screenStageName.value = "YouTube Results: $query"
                    _currentScreenBitmap.value = generateMockScreen(appName, "RESULTS", 1080, 2400, query)
                }
                else -> {
                    _screenStageName.value = "Playing: $query"
                    _currentScreenBitmap.value = generateMockScreen(appName, "PLAYING", 1080, 2400, query)
                }
            }
        } else if (appName.equals("Calculator", ignoreCase = true)) {
            _screenStageName.value = "Calculator Keypad Active"
            _currentScreenBitmap.value = generateMockScreen(appName, "CALC", 1080, 2400, "50 + 25 = 75")
        } else if (appName.equals("Camera", ignoreCase = true)) {
            _screenStageName.value = "Camera Viewfinder"
            _currentScreenBitmap.value = generateMockScreen(appName, "CAMERA", 1080, 2400)
        } else {
            _screenStageName.value = "$appName Screen Active"
            _currentScreenBitmap.value = generateMockScreen(appName, "RESULTS", 1080, 2400, analysis.goal)
        }
    }

    fun replayMacro(macro: Macro) {
        engineJob?.cancel()
        engineJob = coroutineScope.launch(Dispatchers.Default) {
            val service = MacroExecutorService.instance
            val isRealDevice = service != null && !settingsManager.simulationMode.value
            _isDeviceExecuting.value = isRealDevice
            val deviceSize = DeviceDisplayUtils.getRealDisplaySize(context)

            try {
                _engineState.value = EngineState.REPLAYING
                _logs.value = emptyList()
                _activeMacro.value = macro.copy(
                    actions = macro.actions.map { it.copy(status = BlockStatus.PENDING) }
                )
                addLog(1, "Replaying Macro \"${macro.title}\" on ${if (isRealDevice) "Physical Device" else "Simulator"}", LogType.INFO)

                for (block in macro.actions) {
                    _currentStepIndex.value = block.stepNumber
                    markBlockStatus(block.id, BlockStatus.EXECUTING)
                    addLog(block.stepNumber, "Executing Block #${block.stepNumber}: ${block.title}", LogType.INFO)

                    when (block.type) {
                        ActionBlockType.LAUNCH_APP -> {
                            val pkg = block.params["packageName"] ?: "com.google.android.youtube"
                            val appName = block.params["appName"] ?: "App"
                            if (isRealDevice) {
                                service?.showFloatingOverlay("Replaying: Launching $appName...") { stopExecution() }
                                service?.launchTargetApp(pkg)
                            }
                            _screenStageName.value = "$appName Main"
                            _currentScreenBitmap.value = generateMockScreen(appName, "HOME", 1080, 2400)
                            delay(1200L)
                        }

                        ActionBlockType.TAP_COORDINATES -> {
                            val x = block.params["x"]?.toIntOrNull() ?: 540
                            val y = block.params["y"]?.toIntOrNull() ?: 1200
                            val label = block.params["button"] ?: block.params["label"] ?: "Target"

                            val (realX, realY) = DeviceDisplayUtils.mapCoordinatesToDevice(
                                aiX = x,
                                aiY = y,
                                referenceWidth = 1080,
                                referenceHeight = 2400,
                                deviceSize = deviceSize
                            )

                            _currentCrosshair.value = TargetCrosshair(x, y, label, block.stepNumber)
                            if (isRealDevice) {
                                service?.showFloatingOverlay("Replaying: Tap $label...") { stopExecution() }
                                service?.dispatchTapSuspend(realX, realY)
                            }
                            delay(settingsManager.stepDelayMs.value)
                        }

                        ActionBlockType.INPUT_TEXT -> {
                            val text = block.params["text"] ?: ""
                            val pressEnter = block.params["pressEnter"]?.toBoolean() ?: true
                            if (isRealDevice) {
                                service?.showFloatingOverlay("Replaying: Type \"$text\"...") { stopExecution() }
                                service?.inputTextOnDevice(text, pressEnter)
                            }
                            delay(settingsManager.stepDelayMs.value)
                        }

                        ActionBlockType.WAIT_BEFORE_NEXT -> {
                            val seconds = block.params["seconds"]?.toIntOrNull() ?: 2
                            delay(seconds * 1000L)
                        }

                        ActionBlockType.SWIPE_COORDINATES -> {
                            val x1 = block.params["x1"]?.toFloatOrNull() ?: 540f
                            val y1 = block.params["y1"]?.toFloatOrNull() ?: 1600f
                            val x2 = block.params["x2"]?.toFloatOrNull() ?: 540f
                            val y2 = block.params["y2"]?.toFloatOrNull() ?: 600f
                            if (isRealDevice) {
                                service?.dispatchSwipeSuspend(x1, y1, x2, y2)
                            }
                            delay(settingsManager.stepDelayMs.value)
                        }

                        else -> {
                            delay(400L)
                        }
                    }

                    markBlockStatus(block.id, BlockStatus.COMPLETED, 85L, "Replay Success")
                }

                _activeMacro.value = macro.copy(
                    executionCount = macro.executionCount + 1,
                    lastExecutedAt = System.currentTimeMillis()
                )
                repository.saveMacro(
                    macro.copy(
                        executionCount = macro.executionCount + 1,
                        lastExecutedAt = System.currentTimeMillis()
                    )
                )
                addLog(macro.actions.size, "Replay complete on device for all action blocks!", LogType.EXECUTION_SUCCESS)
                _engineState.value = EngineState.COMPLETED
            } catch (e: Exception) {
                Log.e(TAG, "Replay error", e)
                addLog(_currentStepIndex.value, "Replay error on device: ${e.message}", LogType.EXECUTION_ERROR)
                _engineState.value = EngineState.ERROR
                service?.hideFloatingOverlay()
            } finally {
                _isDeviceExecuting.value = false
            }
        }
    }

    fun stopExecution() {
        engineJob?.cancel()
        _engineState.value = EngineState.IDLE
        _isDeviceExecuting.value = false
        MacroExecutorService.instance?.hideFloatingOverlay()
        addLog(_currentStepIndex.value, "Macro execution stopped by user", LogType.EXECUTION_WARNING)
    }

    private fun appendActionBlock(block: ActionBlock) {
        val current = _activeMacro.value ?: return
        val updated = current.copy(actions = current.actions + block)
        _activeMacro.value = updated
    }

    private fun markBlockStatus(
        blockId: String,
        status: BlockStatus,
        executionTimeMs: Long? = null,
        resultMsg: String? = null
    ) {
        val current = _activeMacro.value ?: return
        val updatedActions = current.actions.map { block ->
            if (block.id == blockId) {
                block.copy(
                    status = status,
                    executionTimeMs = executionTimeMs ?: block.executionTimeMs,
                    resultMessage = resultMsg ?: block.resultMessage
                )
            } else block
        }
        _activeMacro.value = current.copy(actions = updatedActions)
    }

    private fun addLog(
        step: Int,
        message: String,
        type: LogType = LogType.INFO,
        coordX: Int? = null,
        coordY: Int? = null
    ) {
        val entry = ExecutionLog(
            step = step,
            message = message,
            type = type,
            coordinateX = coordX,
            coordinateY = coordY
        )
        _logs.value = listOf(entry) + _logs.value
    }

    private fun generateMockScreen(
        appName: String,
        state: String?,
        width: Int,
        height: Int,
        query: String = "Candy Crush"
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply { color = Color.parseColor("#0F141C") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Status bar
        val statusPaint = Paint().apply { color = Color.parseColor("#1B2330") }
        canvas.drawRect(0f, 0f, width.toFloat(), 90f, statusPaint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 34f
            isAntiAlias = true
        }
        canvas.drawText("12:00", 60f, 60f, textPaint)
        canvas.drawText("5G  100%", width - 240f, 60f, textPaint)

        val isPlayStore = appName.contains("Play Store", ignoreCase = true) || appName.contains("vending", ignoreCase = true)
        val isWhatsApp = appName.equals("WhatsApp", ignoreCase = true)

        if (isPlayStore) {
            // Google Play Store Dark UI
            val headerPaint = Paint().apply { color = Color.parseColor("#1F2326") }
            canvas.drawRect(0f, 90f, width.toFloat(), 240f, headerPaint)

            // Search Bar at Top (X: 60 to width - 60, Y: 110 to 210) -> center is (540, 160)
            val barPaint = Paint().apply { color = Color.parseColor("#2D3238") }
            canvas.drawRoundRect(RectF(60f, 110f, width - 60f, 210f), 50f, 50f, barPaint)

            // Magnifying Glass
            val searchIconPaint = Paint().apply { color = Color.parseColor("#9AA0A6"); style = Paint.Style.STROKE; strokeWidth = 5f; isAntiAlias = true }
            canvas.drawCircle(110f, 160f, 18f, searchIconPaint)
            canvas.drawLine(124f, 174f, 140f, 190f, searchIconPaint)

            val hintPaint = Paint().apply { color = Color.parseColor("#9AA0A6"); textSize = 38f; isAntiAlias = true }
            val displayQuery = if (query.isNotBlank()) query else "Candy Crush"

            if (state == "SEARCH_FOCUSED") {
                canvas.drawText(displayQuery, 170f, 174f, textPaint)
                // Cursor
                val cursorPaint = Paint().apply { color = Color.parseColor("#00875A"); strokeWidth = 4f }
                val textWidth = textPaint.measureText(displayQuery)
                canvas.drawLine(170f + textWidth + 8f, 130f, 170f + textWidth + 8f, 190f, cursorPaint)

                // Virtual keyboard on bottom half
                val kbPaint = Paint().apply { color = Color.parseColor("#181B1F") }
                canvas.drawRect(0f, height - 900f, width.toFloat(), height.toFloat(), kbPaint)
                canvas.drawText("⌨ Virtual Keyboard Active (Press Enter to Search)", 120f, height - 450f, hintPaint)
            } else {
                canvas.drawText(if (state == "RESULTS" || state == "DOWNLOADING" || state == "INSTALLED") displayQuery else "Search apps & games", 170f, 174f, hintPaint)
            }

            // Category Bar
            val tabPaint = Paint().apply { color = Color.parseColor("#00875A"); textSize = 34f; isFakeBoldText = true; isAntiAlias = true }
            val inactiveTabPaint = Paint().apply { color = Color.parseColor("#9AA0A6"); textSize = 34f; isAntiAlias = true }
            canvas.drawText("For you", 80f, 290f, tabPaint)
            canvas.drawText("Top charts", 260f, 290f, inactiveTabPaint)
            canvas.drawText("Kids", 480f, 290f, inactiveTabPaint)
            canvas.drawText("Categories", 620f, 290f, inactiveTabPaint)

            when (state) {
                "RESULTS", "DOWNLOADING", "INSTALLED" -> {
                    // App Search Result Card for Candy Crush
                    val cardBg = Paint().apply { color = Color.parseColor("#1A1D21") }
                    canvas.drawRoundRect(RectF(40f, 330f, width - 40f, 780f), 28f, 28f, cardBg)

                    // Candy Crush App Icon (colorful match-3 candy emblem)
                    val iconBg = Paint().apply { color = Color.parseColor("#E11D48") }
                    canvas.drawRoundRect(RectF(70f, 360f, 230f, 520f), 32f, 32f, iconBg)
                    // Yellow candy circle
                    val candyYellow = Paint().apply { color = Color.parseColor("#FBBF24") }
                    canvas.drawCircle(130f, 420f, 28f, candyYellow)
                    // Cyan candy diamond
                    val candyCyan = Paint().apply { color = Color.parseColor("#38BDF8") }
                    canvas.drawCircle(170f, 460f, 24f, candyCyan)

                    val appTitle = Paint().apply { color = Color.WHITE; textSize = 42f; isFakeBoldText = true; isAntiAlias = true }
                    canvas.drawText("$displayQuery Saga", 260f, 410f, appTitle)

                    val devText = Paint().apply { color = Color.parseColor("#9AA0A6"); textSize = 30f; isAntiAlias = true }
                    canvas.drawText("King • Puzzle • Match 3", 260f, 455f, devText)
                    canvas.drawText("4.6 ★  •  1B+ Downloads  •  Rated 3+", 260f, 500f, devText)

                    // Install Button (X: 740 to 980, Y: 380 to 460) -> center is (880, 420)
                    if (state == "DOWNLOADING") {
                        val dlPaint = Paint().apply { color = Color.parseColor("#00875A") }
                        canvas.drawRoundRect(RectF(720f, 375f, width - 60f, 465f), 45f, 45f, dlPaint)
                        val dlText = Paint().apply { color = Color.WHITE; textSize = 34f; isFakeBoldText = true; isAntiAlias = true }
                        canvas.drawText("Downloading 68%", 740f, 432f, dlText)

                        // Progress bar below card
                        val pbBg = Paint().apply { color = Color.parseColor("#2D3238") }
                        canvas.drawRoundRect(RectF(70f, 560f, width - 70f, 580f), 10f, 10f, pbBg)
                        val pbFg = Paint().apply { color = Color.parseColor("#00875A") }
                        canvas.drawRoundRect(RectF(70f, 560f, (width - 70f) * 0.68f, 580f), 10f, 10f, pbFg)
                    } else if (state == "INSTALLED") {
                        val playPaint = Paint().apply { color = Color.parseColor("#00875A") }
                        canvas.drawRoundRect(RectF(780f, 375f, width - 60f, 465f), 45f, 45f, playPaint)
                        val playText = Paint().apply { color = Color.WHITE; textSize = 36f; isFakeBoldText = true; isAntiAlias = true }
                        canvas.drawText("Play ▶", 830f, 432f, playText)
                    } else {
                        // Green Install Button
                        val installBtn = Paint().apply { color = Color.parseColor("#00875A") }
                        canvas.drawRoundRect(RectF(760f, 375f, width - 60f, 465f), 45f, 45f, installBtn)
                        val installText = Paint().apply { color = Color.WHITE; textSize = 38f; isFakeBoldText = true; isAntiAlias = true }
                        canvas.drawText("Install", 820f, 432f, installText)
                    }

                    // Secondary cards (Candy Crush Soda Saga)
                    val card2 = Paint().apply { color = Color.parseColor("#15181C") }
                    canvas.drawRoundRect(RectF(40f, 820f, width - 40f, 1200f), 28f, 28f, card2)
                    canvas.drawRoundRect(RectF(70f, 850f, 230f, 1010f), 32f, 32f, Paint().apply { color = Color.parseColor("#8B5CF6") })
                    canvas.drawText("Candy Crush Soda Saga", 260f, 900f, appTitle)
                    canvas.drawText("King • 4.7 ★ • 500M+ Downloads", 260f, 950f, devText)
                }
                else -> {
                    // Play Store Home Feed
                    val bannerPaint = Paint().apply { color = Color.parseColor("#1A2333") }
                    canvas.drawRoundRect(RectF(40f, 340f, width - 40f, 880f), 28f, 28f, bannerPaint)

                    val featureText = Paint().apply { color = Color.WHITE; textSize = 48f; isFakeBoldText = true; isAntiAlias = true }
                    canvas.drawText("Featured: $displayQuery Saga", 80f, 420f, featureText)
                    val subText = Paint().apply { color = Color.parseColor("#9AA0A6"); textSize = 34f }
                    canvas.drawText("Join millions worldwide in the sweet puzzle adventure!", 80f, 480f, subText)

                    // Install button on banner
                    val btnPaint = Paint().apply { color = Color.parseColor("#00875A") }
                    canvas.drawRoundRect(RectF(80f, 520f, 320f, 600f), 40f, 40f, btnPaint)
                    canvas.drawText("Install", 140f, 572f, Paint().apply { color = Color.WHITE; textSize = 36f; isFakeBoldText = true })

                    // Suggested Games Grid
                    val gridCard = Paint().apply { color = Color.parseColor("#14181E") }
                    canvas.drawRoundRect(RectF(40f, 920f, width - 40f, 1500f), 28f, 28f, gridCard)
                    canvas.drawText("Recommended For You", 80f, 990f, featureText)
                }
            }
        } else if (isWhatsApp) {
            // WhatsApp Header (Dark Emerald #1F2C34)
            val waHeaderPaint = Paint().apply { color = Color.parseColor("#1F2C34") }
            canvas.drawRect(0f, 90f, width.toFloat(), 240f, waHeaderPaint)

            val waGreen = Paint().apply { color = Color.parseColor("#00A884"); textSize = 50f; isFakeBoldText = true; isAntiAlias = true }
            val waWhite = Paint().apply { color = Color.WHITE; textSize = 44f; isFakeBoldText = true; isAntiAlias = true }

            when (state) {
                "SEARCH_FOCUSED" -> {
                    val barPaint = Paint().apply { color = Color.parseColor("#2A3942") }
                    canvas.drawRoundRect(RectF(120f, 110f, width - 60f, 210f), 45f, 45f, barPaint)

                    val hintPaint = Paint().apply { color = Color.parseColor("#8696A0"); textSize = 38f; isAntiAlias = true }
                    canvas.drawText(if (query.isNotBlank()) "Search: $query" else "Search contacts...", 160f, 175f, hintPaint)

                    val rowPaint = Paint().apply { color = Color.parseColor("#121B22") }
                    canvas.drawRect(0f, 260f, width.toFloat(), 480f, rowPaint)

                    val avatarPaint = Paint().apply { color = Color.parseColor("#00A884") }
                    canvas.drawCircle(120f, 370f, 50f, avatarPaint)
                    canvas.drawText("S", 106f, 386f, textPaint)

                    val contactName = if (query.isNotBlank()) query.uppercase() else "SHAKEER"
                    canvas.drawText(contactName, 200f, 360f, waWhite)
                    canvas.drawText("Hey there! I am using WhatsApp.", 200f, 410f, hintPaint)
                }
                "CHAT_OPEN", "MESSAGE_SENT" -> {
                    val parts = query.split(":")
                    val contactName = parts.getOrNull(0)?.ifBlank { "shakeer" } ?: "shakeer"
                    val messageText = parts.getOrNull(1)?.ifBlank { "hi" } ?: "hi"

                    canvas.drawText("← $contactName", 60f, 175f, waWhite)
                    val onlinePaint = Paint().apply { color = Color.parseColor("#8696A0"); textSize = 28f }
                    canvas.drawText("online", 160f, 215f, onlinePaint)

                    val chatBgPaint = Paint().apply { color = Color.parseColor("#0B141A") }
                    canvas.drawRect(0f, 240f, width.toFloat(), height - 160f, chatBgPaint)

                    if (state == "MESSAGE_SENT") {
                        val bubblePaint = Paint().apply { color = Color.parseColor("#005C4B") }
                        canvas.drawRoundRect(RectF(width - 400f, 600f, width - 60f, 740f), 24f, 24f, bubblePaint)
                        canvas.drawText(messageText, width - 360f, 680f, textPaint)
                        val checkPaint = Paint().apply { color = Color.parseColor("#53BDEB"); textSize = 28f }
                        canvas.drawText("12:01 ✓✓", width - 200f, 720f, checkPaint)
                    }

                    val inputBgPaint = Paint().apply { color = Color.parseColor("#1F2C34") }
                    canvas.drawRoundRect(RectF(30f, height - 150f, width - 150f, height - 30f), 60f, 60f, inputBgPaint)
                    val inputHint = Paint().apply { color = Color.parseColor("#8696A0"); textSize = 36f }
                    canvas.drawText(if (state == "CHAT_OPEN") messageText else "Message", 80f, height - 75f, inputHint)

                    val sendBtnPaint = Paint().apply { color = Color.parseColor("#00A884") }
                    canvas.drawCircle(width - 80f, height - 90f, 55f, sendBtnPaint)
                    val sendIconPaint = Paint().apply { color = Color.WHITE; textSize = 40f; isFakeBoldText = true }
                    canvas.drawText("➤", width - 98f, height - 75f, sendIconPaint)
                }
                else -> {
                    canvas.drawText("WhatsApp", 60f, 175f, waGreen)

                    val searchIconPaint = Paint().apply { color = Color.parseColor("#8696A0"); style = Paint.Style.STROKE; strokeWidth = 6f; isAntiAlias = true }
                    canvas.drawCircle(width - 195f, 156f, 24f, searchIconPaint)
                    canvas.drawLine(width - 178f, 173f, width - 160f, 191f, searchIconPaint)

                    canvas.drawCircle(width - 80f, 156f, 6f, Paint().apply { color = Color.parseColor("#8696A0") })

                    val chatRowPaint = Paint().apply { color = Color.parseColor("#121B22") }
                    val borderPaint = Paint().apply { color = Color.parseColor("#1F2C34") }
                    for (i in 0..4) {
                        val yTop = 270f + (i * 180f)
                        canvas.drawRect(0f, yTop, width.toFloat(), yTop + 175f, chatRowPaint)
                        canvas.drawLine(180f, yTop + 175f, width.toFloat(), yTop + 175f, borderPaint)
                        canvas.drawCircle(100f, yTop + 85f, 45f, Paint().apply { color = Color.parseColor("#2A3942") })
                    }
                }
            }
        } else if (appName.equals("Calculator", ignoreCase = true)) {
            val calcBg = Paint().apply { color = Color.parseColor("#1E293B") }
            canvas.drawRect(0f, 90f, width.toFloat(), height.toFloat(), calcBg)

            val displayPaint = Paint().apply { color = Color.WHITE; textSize = 72f; isFakeBoldText = true }
            canvas.drawText(query.ifBlank { "0" }, 100f, 400f, displayPaint)

            val keyBg = Paint().apply { color = Color.parseColor("#334155") }
            val keyOpBg = Paint().apply { color = Color.parseColor("#2563EB") }
            val keyText = Paint().apply { color = Color.WHITE; textSize = 48f; isFakeBoldText = true }

            val keys = listOf(
                listOf("7", "8", "9", "÷"),
                listOf("4", "5", "6", "×"),
                listOf("1", "2", "3", "-"),
                listOf("0", ".", "=", "+")
            )

            for (r in keys.indices) {
                for (c in keys[r].indices) {
                    val kx = 60f + (c * 240f)
                    val ky = 600f + (r * 220f)
                    val isOp = c == 3 || (r == 3 && c == 2)
                    canvas.drawRoundRect(RectF(kx, ky, kx + 210f, ky + 190f), 30f, 30f, if (isOp) keyOpBg else keyBg)
                    canvas.drawText(keys[r][c], kx + 85f, ky + 120f, keyText)
                }
            }
        } else {
            // General App / YouTube Header
            val headerPaint = Paint().apply { color = Color.parseColor("#17202E") }
            canvas.drawRect(0f, 90f, width.toFloat(), 240f, headerPaint)

            val titlePaint = Paint().apply {
                color = Color.parseColor("#60A5FA")
                textSize = 52f
                isFakeBoldText = true
                isAntiAlias = true
            }

            when (state) {
                "SEARCH_FOCUSED" -> {
                    val barPaint = Paint().apply { color = Color.parseColor("#26354D") }
                    canvas.drawRoundRect(RectF(120f, 110f, width - 60f, 210f), 45f, 45f, barPaint)

                    val hintPaint = Paint().apply {
                        color = Color.parseColor("#94A3B8")
                        textSize = 40f
                        isAntiAlias = true
                    }
                    canvas.drawText("Search $appName...", 160f, 175f, hintPaint)

                    val kbPaint = Paint().apply { color = Color.parseColor("#1E293B") }
                    canvas.drawRect(0f, height - 900f, width.toFloat(), height.toFloat(), kbPaint)
                    canvas.drawText("⌨ Virtual Keyboard Focused", width / 2f - 240f, height - 450f, titlePaint)
                }
                "RESULTS" -> {
                    canvas.drawText("$appName: \"$query\"", 60f, 180f, titlePaint)

                    val cardPaint = Paint().apply { color = Color.parseColor("#1A2433") }
                    val thumbPaint = Paint().apply { color = Color.parseColor("#2563EB") }
                    val subText = Paint().apply { color = Color.parseColor("#94A3B8"); textSize = 36f }

                    canvas.drawRoundRect(RectF(60f, 280f, width - 60f, 850f), 24f, 24f, thumbPaint)
                    canvas.drawText("▶ PLAY: $query (Latest Episode)", 100f, 580f, textPaint.apply { textSize = 44f; isFakeBoldText = true })
                    canvas.drawRoundRect(RectF(60f, 860f, width - 60f, 1020f), 16f, 16f, cardPaint)
                    canvas.drawText("$query • 2.4M views • 2 hours ago", 80f, 940f, subText)

                    canvas.drawRoundRect(RectF(60f, 1060f, width - 60f, 1600f), 24f, 24f, thumbPaint)
                    canvas.drawText("▶ $query Highlights & Behind the Scenes", 100f, 1340f, textPaint)
                }
                "PLAYING" -> {
                    canvas.drawText("▶ Playing: $query", 60f, 180f, titlePaint)

                    val playerPaint = Paint().apply { color = Color.parseColor("#000000") }
                    canvas.drawRect(0f, 240f, width.toFloat(), 950f, playerPaint)

                    val playOverlay = Paint().apply { color = Color.parseColor("#EF4444"); textSize = 64f; isFakeBoldText = true }
                    canvas.drawText("▶ VIDEO STREAMING ACTIVE", width / 2f - 380f, 600f, playOverlay)

                    val progressBg = Paint().apply { color = Color.GRAY }
                    val progressFg = Paint().apply { color = Color.RED }
                    canvas.drawRect(60f, 920f, width - 60f, 936f, progressBg)
                    canvas.drawRect(60f, 920f, width * 0.45f, 936f, progressFg)

                    val descPaint = Paint().apply { color = Color.WHITE; textSize = 42f; isFakeBoldText = true }
                    canvas.drawText("$query - Official Episode", 60f, 1020f, descPaint)
                }
                else -> {
                    canvas.drawText(appName.uppercase(), 60f, 180f, titlePaint)

                    val iconPaint = Paint().apply {
                        color = Color.parseColor("#38BDF8")
                        style = Paint.Style.STROKE
                        strokeWidth = 8f
                        isAntiAlias = true
                    }
                    canvas.drawCircle(width - 140f, 165f, 32f, iconPaint)
                    canvas.drawLine(width - 118f, 187f, width - 96f, 209f, iconPaint)

                    val cardPaint = Paint().apply { color = Color.parseColor("#16202C") }
                    canvas.drawRoundRect(RectF(60f, 280f, width - 60f, 900f), 24f, 24f, cardPaint)
                    canvas.drawRoundRect(RectF(60f, 950f, width - 60f, 1570f), 24f, 24f, cardPaint)
                    canvas.drawRoundRect(RectF(60f, 1620f, width - 60f, 2240f), 24f, 24f, cardPaint)
                }
            }
        }

        return bitmap
    }
}
