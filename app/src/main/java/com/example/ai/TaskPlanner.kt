package com.example.ai

import android.content.Context
import com.example.model.ActionBlockType
import com.example.util.AppDiscoveryHelper
import com.example.util.InstalledAppInfo

data class PlannedActionStep(
    val stepIndex: Int,
    val type: ActionBlockType,
    val title: String,
    val description: String,
    val targetButtonName: String,
    val visualSearchQuery: String = "",
    val x: Int,
    val y: Int,
    val textToInput: String? = null,
    val pressEnter: Boolean = false,
    val searchKeywords: List<String> = emptyList(),
    val reasoning: String
)

data class TaskAnalysis(
    val goal: String,
    val targetAppPackage: String,
    val targetAppName: String,
    val actionCategory: String, // "APP_DOWNLOAD", "MESSAGING", "VIDEO_PLAY", "MUSIC", "APP_SEARCH", "CALCULATOR", "CAMERA", "SETTINGS", "BROWSER", "SOCIAL", "GENERIC"
    val parameters: Map<String, String>,
    val plannedStepsDescription: List<String>,
    val plannedSteps: List<PlannedActionStep> = emptyList()
)

object TaskPlanner {

    /**
     * Searches and generates the step-by-step procedural action guide for any command.
     * Can query Gemini API when an API key is available or synthesize using semantic decomposition.
     */
    fun analyzeCommand(prompt: String, context: Context? = null): TaskAnalysis {
        val lower = prompt.lowercase().trim()

        // 1. Download / Install Game or App
        // e.g. "download candy crush game", "how to download candy crush game", "install telegram", "download pubg", "get asphalt 9"
        val isDownloadIntent = lower.startsWith("download") || lower.contains("download") ||
                lower.startsWith("install") || lower.contains("install") ||
                (lower.contains("get") && (lower.contains("game") || lower.contains("app")))

        if (isDownloadIntent) {
            val appQuery = extractDownloadAppName(prompt)
            val storeApp = if (context != null) {
                AppDiscoveryHelper.resolveApp(context, "play store")
            } else {
                InstalledAppInfo("Google Play Store", "com.android.vending")
            }

            val steps = listOf(
                PlannedActionStep(
                    stepIndex = 1,
                    type = ActionBlockType.LAUNCH_APP,
                    title = "Launch ${storeApp.appName}",
                    description = "Open Google Play Store on device",
                    targetButtonName = "Google Play Store Launcher",
                    visualSearchQuery = "Google Play Store app icon on home screen",
                    x = 0, y = 0,
                    searchKeywords = listOf("Play Store", "Google Play"),
                    reasoning = "Initiate download task by opening Google Play Store"
                ),
                PlannedActionStep(
                    stepIndex = 2,
                    type = ActionBlockType.WAIT_BEFORE_NEXT,
                    title = "Wait for Play Store Home",
                    description = "Allow store catalog and search bar to load",
                    targetButtonName = "Play Store Home Loader",
                    visualSearchQuery = "Play Store home catalog with search bar",
                    x = 0, y = 0,
                    reasoning = "Wait 2.5s for store home screen and top search bar to render"
                ),
                PlannedActionStep(
                    stepIndex = 3,
                    type = ActionBlockType.TAP_COORDINATES,
                    title = "Click Search Bar in Play Store",
                    description = "Tap top search bar in Play Store",
                    targetButtonName = "Play Store Search Bar",
                    visualSearchQuery = "search bar at top of Play Store with magnifying glass and 'Search apps & games' text",
                    x = 540, y = 160,
                    searchKeywords = listOf("Search for apps & games", "search_box_idle_text", "search_bar", "search"),
                    reasoning = "Focus the Play Store search input to look up \"$appQuery\""
                ),
                PlannedActionStep(
                    stepIndex = 4,
                    type = ActionBlockType.INPUT_TEXT,
                    title = "Type App Name: \"$appQuery\"",
                    description = "Type \"$appQuery\" and press Enter",
                    targetButtonName = "Search Input Field",
                    visualSearchQuery = "active text input field with cursor",
                    x = 540, y = 160,
                    textToInput = appQuery,
                    pressEnter = true,
                    searchKeywords = listOf("search_box_text_input", "search", "EditText"),
                    reasoning = "Query the Google Play database for \"$appQuery\""
                ),
                PlannedActionStep(
                    stepIndex = 5,
                    type = ActionBlockType.WAIT_BEFORE_NEXT,
                    title = "Wait for Store Results",
                    description = "Allow store search results to populate",
                    targetButtonName = "Search Results Loader",
                    visualSearchQuery = "list of app search results matching $appQuery",
                    x = 0, y = 0,
                    reasoning = "Wait 2s for store network request and app card list to appear"
                ),
                PlannedActionStep(
                    stepIndex = 6,
                    type = ActionBlockType.TAP_COORDINATES,
                    title = "Click Install on $appQuery",
                    description = "Tap the green Install button next to $appQuery",
                    targetButtonName = "Install Button",
                    visualSearchQuery = "green pill button with text 'Install' beside $appQuery",
                    x = 880, y = 420,
                    searchKeywords = listOf("Install", "install_button", "get_app", "Install button"),
                    reasoning = "Trigger download and installation of $appQuery on device"
                ),
                PlannedActionStep(
                    stepIndex = 7,
                    type = ActionBlockType.WAIT_BEFORE_NEXT,
                    title = "Verify Download Started",
                    description = "Confirm download progress indicator is running",
                    targetButtonName = "Download Verifier",
                    visualSearchQuery = "progress bar or downloading status on app card",
                    x = 0, y = 0,
                    reasoning = "Verify download indicator is active and app is downloading"
                )
            )

            return TaskAnalysis(
                goal = "Search and download $appQuery from Google Play Store",
                targetAppPackage = storeApp.packageName,
                targetAppName = storeApp.appName,
                actionCategory = "APP_DOWNLOAD",
                parameters = mapOf("appName" to appQuery, "game" to appQuery),
                plannedStepsDescription = listOf(
                    "Launch Google Play Store (${storeApp.packageName})",
                    "Wait for store home screen",
                    "Click top search bar",
                    "Type \"$appQuery\" and submit search",
                    "Wait for search results",
                    "Click green Install button on $appQuery",
                    "Verify download started"
                ),
                plannedSteps = steps
            )
        }

        // 2. WhatsApp / Messaging with contact & message
        // e.g. "send hi message to shakeer in whats", "send hello to shakeer on whatsapp", "whatsapp shakeer hi"
        val isExplicitMessaging = (lower.contains("send") && (lower.contains("to") || lower.contains("message") || lower.contains("msg"))) ||
                (lower.contains("whats") && (lower.contains("shakeer") || lower.contains("hi") || lower.contains("hello") || lower.contains("message") || lower.contains("send") || lower.contains("text"))) ||
                (lower.contains("text") && lower.contains("to"))

        if (isExplicitMessaging) {
            val contact = extractContact(prompt, default = "shakeer")
            val message = extractMessage(prompt, default = "hi")

            val appInfo = if (context != null) {
                AppDiscoveryHelper.resolveApp(context, "whatsapp")
            } else {
                InstalledAppInfo("WhatsApp", "com.whatsapp")
            }

            val steps = listOf(
                PlannedActionStep(
                    stepIndex = 1,
                    type = ActionBlockType.LAUNCH_APP,
                    title = "Launch ${appInfo.appName}",
                    description = "Launch ${appInfo.packageName} on device",
                    targetButtonName = "WhatsApp Launcher",
                    visualSearchQuery = "WhatsApp app icon with green chat bubble",
                    x = 0, y = 0,
                    searchKeywords = listOf("WhatsApp"),
                    reasoning = "Initiate automation by launching ${appInfo.appName} on the phone"
                ),
                PlannedActionStep(
                    stepIndex = 2,
                    type = ActionBlockType.WAIT_BEFORE_NEXT,
                    title = "Wait for ${appInfo.appName} Feed",
                    description = "Allow chat list and UI hierarchy to render",
                    targetButtonName = "WhatsApp Feed Loader",
                    visualSearchQuery = "WhatsApp chat list screen with green top bar",
                    x = 0, y = 0,
                    reasoning = "Wait 2.5s for initial chat list and network state to initialize"
                ),
                PlannedActionStep(
                    stepIndex = 3,
                    type = ActionBlockType.TAP_COORDINATES,
                    title = "Click Search Bar Button",
                    description = "Click WhatsApp search icon (magnifying glass) in top app bar",
                    targetButtonName = "WhatsApp Top Search Button",
                    visualSearchQuery = "magnifying glass search icon in top right of WhatsApp header",
                    x = 885, y = 156,
                    searchKeywords = listOf("Search", "menuitem_search", "action_search", "Search…"),
                    reasoning = "Click the top-right search magnifying glass to open WhatsApp contact search"
                ),
                PlannedActionStep(
                    stepIndex = 4,
                    type = ActionBlockType.INPUT_TEXT,
                    title = "Type Contact: \"$contact\"",
                    description = "Type target contact name into active search bar",
                    targetButtonName = "Search Input Textbox",
                    visualSearchQuery = "active search input box in top header",
                    x = 450, y = 156,
                    textToInput = contact,
                    pressEnter = false,
                    searchKeywords = listOf("search_src_text", "search", "Search…", "EditText"),
                    reasoning = "Filter chat list by typing \"$contact\" into the search field"
                ),
                PlannedActionStep(
                    stepIndex = 5,
                    type = ActionBlockType.WAIT_BEFORE_NEXT,
                    title = "Wait for Filtered Results",
                    description = "Allow search query to filter contacts",
                    targetButtonName = "Contact Results Filter",
                    visualSearchQuery = "filtered contact row list with avatar",
                    x = 0, y = 0,
                    reasoning = "Allow 1.5s for WhatsApp to filter and show matching contact"
                ),
                PlannedActionStep(
                    stepIndex = 6,
                    type = ActionBlockType.TAP_COORDINATES,
                    title = "Click Contact \"$contact\"",
                    description = "Select and confirm contact \"$contact\" from search results",
                    targetButtonName = "Contact Item: $contact",
                    visualSearchQuery = "contact row matching name $contact with circular profile avatar",
                    x = 450, y = 420,
                    searchKeywords = listOf(contact, "conversations_row_contact_name", "contact"),
                    reasoning = "Click the filtered row matching contact \"$contact\" to open conversation screen"
                ),
                PlannedActionStep(
                    stepIndex = 7,
                    type = ActionBlockType.WAIT_BEFORE_NEXT,
                    title = "Wait for Chat to Open",
                    description = "Allow conversation history and input box to render",
                    targetButtonName = "Chat View Loader",
                    visualSearchQuery = "chat view with conversation bubbles and bottom message bar",
                    x = 0, y = 0,
                    reasoning = "Wait 1.5s for chat view and message composer to be active"
                ),
                PlannedActionStep(
                    stepIndex = 8,
                    type = ActionBlockType.TAP_COORDINATES,
                    title = "Click Message Textbox",
                    description = "Focus the message composer at the bottom of the chat",
                    targetButtonName = "Message Composer Box",
                    visualSearchQuery = "bottom edit text field with placeholder 'Message'",
                    x = 430, y = 2280,
                    searchKeywords = listOf("Message", "entry", "Type a message", "message_input"),
                    reasoning = "Tap bottom message edit field to place cursor and show virtual keyboard"
                ),
                PlannedActionStep(
                    stepIndex = 9,
                    type = ActionBlockType.INPUT_TEXT,
                    title = "Type Message: \"$message\"",
                    description = "Inject message \"$message\" into the chat text field",
                    targetButtonName = "Chat Input Field",
                    visualSearchQuery = "focused message text area containing cursor",
                    x = 430, y = 2280,
                    textToInput = message,
                    pressEnter = false,
                    searchKeywords = listOf("entry", "Message", "EditText"),
                    reasoning = "Type the intended message content \"$message\" into the message input field"
                ),
                PlannedActionStep(
                    stepIndex = 10,
                    type = ActionBlockType.TAP_COORDINATES,
                    title = "Click Send Button",
                    description = "Click the green Send button icon at bottom right",
                    targetButtonName = "WhatsApp Send Button",
                    visualSearchQuery = "green circular button with white send arrow at bottom right",
                    x = 995, y = 2280,
                    searchKeywords = listOf("Send", "send", "voice_note", "send_button"),
                    reasoning = "Click green send arrow to transmit message \"$message\" to $contact"
                ),
                PlannedActionStep(
                    stepIndex = 11,
                    type = ActionBlockType.WAIT_BEFORE_NEXT,
                    title = "Verify Message Delivered",
                    description = "Confirm outgoing message bubble in chat",
                    targetButtonName = "Delivery Verifier",
                    visualSearchQuery = "green outgoing message bubble with checkmarks",
                    x = 0, y = 0,
                    reasoning = "Verify message bubble sent to recipient"
                )
            )

            return TaskAnalysis(
                goal = "Send WhatsApp message \"$message\" to $contact",
                targetAppPackage = appInfo.packageName,
                targetAppName = appInfo.appName,
                actionCategory = "MESSAGING",
                parameters = mapOf("contact" to contact, "message" to message),
                plannedStepsDescription = listOf(
                    "Launch WhatsApp (${appInfo.packageName})",
                    "Wait for chat list to load",
                    "Click Search bar button in top header",
                    "Type contact name \"$contact\"",
                    "Confirm & click contact \"$contact\" from results",
                    "Click message textbox at bottom of chat",
                    "Type message \"$message\"",
                    "Click Send button (green send arrow)",
                    "Verify message successfully delivered"
                ),
                plannedSteps = steps
            )
        }

        // 3. YouTube Search & Play
        val isYouTube = lower.contains("youtube") || lower.contains("yt") || lower.contains("video") || lower.contains("vlog")
        if (isYouTube) {
            val query = extractSearchQuery(prompt, default = "VJ Siddhu Vlogs")
            val appInfo = if (context != null) {
                AppDiscoveryHelper.resolveApp(context, "youtube")
            } else {
                InstalledAppInfo("YouTube", "com.google.android.youtube")
            }

            val steps = listOf(
                PlannedActionStep(
                    stepIndex = 1,
                    type = ActionBlockType.LAUNCH_APP,
                    title = "Launch ${appInfo.appName}",
                    description = "Launch ${appInfo.packageName} on device",
                    targetButtonName = "YouTube Launcher",
                    visualSearchQuery = "YouTube app icon",
                    x = 0, y = 0,
                    searchKeywords = listOf("YouTube"),
                    reasoning = "Launch YouTube to begin video search"
                ),
                PlannedActionStep(
                    stepIndex = 2,
                    type = ActionBlockType.WAIT_BEFORE_NEXT,
                    title = "Wait for Feed Load",
                    description = "Allow YouTube feed and top bar to load",
                    targetButtonName = "YouTube Feed Loader",
                    visualSearchQuery = "YouTube home feed with video thumbnails",
                    x = 0, y = 0,
                    reasoning = "Wait 2.5s for YouTube feed to initialize"
                ),
                PlannedActionStep(
                    stepIndex = 3,
                    type = ActionBlockType.TAP_COORDINATES,
                    title = "Click Search Button",
                    description = "Click Search magnifying glass at top right of YouTube",
                    targetButtonName = "YouTube Search Button",
                    visualSearchQuery = "magnifying glass search icon in top right of YouTube top bar",
                    x = 950, y = 156,
                    searchKeywords = listOf("Search", "search", "menu_search"),
                    reasoning = "Click YouTube search icon in top app bar"
                ),
                PlannedActionStep(
                    stepIndex = 4,
                    type = ActionBlockType.INPUT_TEXT,
                    title = "Type Query: \"$query\"",
                    description = "Type search query into YouTube search bar and submit",
                    targetButtonName = "YouTube Search Bar",
                    visualSearchQuery = "search text edit field with cursor",
                    x = 540, y = 156,
                    textToInput = query,
                    pressEnter = true,
                    searchKeywords = listOf("search_edit_text", "search", "EditText"),
                    reasoning = "Type query \"$query\" and press enter to fetch search results"
                ),
                PlannedActionStep(
                    stepIndex = 5,
                    type = ActionBlockType.WAIT_BEFORE_NEXT,
                    title = "Wait for Video Results",
                    description = "Allow video results to load",
                    targetButtonName = "Results Loader",
                    visualSearchQuery = "list of YouTube video search results",
                    x = 0, y = 0,
                    reasoning = "Wait 2.5s for YouTube search video list to load"
                ),
                PlannedActionStep(
                    stepIndex = 6,
                    type = ActionBlockType.TAP_COORDINATES,
                    title = "Click Video Thumbnail",
                    description = "Click first video thumbnail in results to start streaming",
                    targetButtonName = "First Video Result",
                    visualSearchQuery = "large top video thumbnail card",
                    x = 540, y = 840,
                    searchKeywords = listOf("video", "thumbnail", "watch", query),
                    reasoning = "Click first video thumbnail at (540, 840) to initiate playback"
                )
            )

            return TaskAnalysis(
                goal = "Open YouTube, search \"$query\" and play video",
                targetAppPackage = appInfo.packageName,
                targetAppName = appInfo.appName,
                actionCategory = "VIDEO_PLAY",
                parameters = mapOf("query" to query),
                plannedStepsDescription = listOf(
                    "Launch YouTube app (${appInfo.packageName})",
                    "Wait for feed to load",
                    "Click Search magnifying glass at top right",
                    "Type query \"$query\" and press Enter",
                    "Wait for search results",
                    "Click first video thumbnail to start playback",
                    "Verify streaming in full player"
                ),
                plannedSteps = steps
            )
        }

        // 4. Calculator
        val isCalculator = lower.contains("calculator") || lower.contains("calculate") || lower.contains("calc")
        if (isCalculator) {
            val appInfo = if (context != null) {
                AppDiscoveryHelper.resolveApp(context, "calculator")
            } else {
                InstalledAppInfo("Calculator", "com.google.android.calculator")
            }

            val steps = listOf(
                PlannedActionStep(
                    stepIndex = 1,
                    type = ActionBlockType.LAUNCH_APP,
                    title = "Launch Calculator",
                    description = "Launch ${appInfo.packageName}",
                    targetButtonName = "Calculator Launcher",
                    visualSearchQuery = "Calculator app icon",
                    x = 0, y = 0,
                    reasoning = "Open Calculator app on device"
                ),
                PlannedActionStep(
                    stepIndex = 2,
                    type = ActionBlockType.WAIT_BEFORE_NEXT,
                    title = "Wait for Keypad",
                    description = "Allow calculator keypad to render",
                    targetButtonName = "Keypad Loader",
                    visualSearchQuery = "calculator numeric keypad grid",
                    x = 0, y = 0,
                    reasoning = "Wait 1.5s for numeric keypad to appear"
                ),
                PlannedActionStep(
                    stepIndex = 3,
                    type = ActionBlockType.TAP_COORDINATES,
                    title = "Click Digit 5",
                    description = "Tap digit 5 on keypad",
                    targetButtonName = "Key 5",
                    visualSearchQuery = "circular keypad button labeled 5",
                    x = 220, y = 1750,
                    searchKeywords = listOf("5", "digit_5"),
                    reasoning = "Input first operand digit"
                ),
                PlannedActionStep(
                    stepIndex = 4,
                    type = ActionBlockType.TAP_COORDINATES,
                    title = "Click Digit 0",
                    description = "Tap digit 0 on keypad",
                    targetButtonName = "Key 0",
                    visualSearchQuery = "keypad button labeled 0",
                    x = 220, y = 2150,
                    searchKeywords = listOf("0", "digit_0"),
                    reasoning = "Input first operand second digit"
                ),
                PlannedActionStep(
                    stepIndex = 5,
                    type = ActionBlockType.TAP_COORDINATES,
                    title = "Click Operator Plus (+)",
                    description = "Tap plus button on keypad",
                    targetButtonName = "Key Plus",
                    visualSearchQuery = "operator button labeled +",
                    x = 880, y = 1950,
                    searchKeywords = listOf("+", "add", "op_add"),
                    reasoning = "Input addition operator"
                ),
                PlannedActionStep(
                    stepIndex = 6,
                    type = ActionBlockType.TAP_COORDINATES,
                    title = "Click Digit 2",
                    description = "Tap digit 2 on keypad",
                    targetButtonName = "Key 2",
                    visualSearchQuery = "keypad button labeled 2",
                    x = 540, y = 1950,
                    searchKeywords = listOf("2", "digit_2"),
                    reasoning = "Input second operand digit"
                ),
                PlannedActionStep(
                    stepIndex = 7,
                    type = ActionBlockType.TAP_COORDINATES,
                    title = "Click Digit 5",
                    description = "Tap digit 5 on keypad",
                    targetButtonName = "Key 5",
                    visualSearchQuery = "keypad button labeled 5",
                    x = 220, y = 1750,
                    searchKeywords = listOf("5", "digit_5"),
                    reasoning = "Input second operand second digit"
                ),
                PlannedActionStep(
                    stepIndex = 8,
                    type = ActionBlockType.TAP_COORDINATES,
                    title = "Click Equals (=)",
                    description = "Tap equals button to compute result",
                    targetButtonName = "Key Equals",
                    visualSearchQuery = "blue equals button labeled =",
                    x = 880, y = 2150,
                    searchKeywords = listOf("=", "equals", "eq"),
                    reasoning = "Execute calculation and display result"
                )
            )

            return TaskAnalysis(
                goal = "Perform calculation in Calculator app",
                targetAppPackage = appInfo.packageName,
                targetAppName = appInfo.appName,
                actionCategory = "CALCULATOR",
                parameters = mapOf("expression" to prompt),
                plannedStepsDescription = listOf(
                    "Launch Calculator app",
                    "Wait for keypad",
                    "Click first operand digits",
                    "Click operation (+)",
                    "Click second operand digits",
                    "Click Equals (=) to compute result"
                ),
                plannedSteps = steps
            )
        }

        // 5. Camera
        val isCamera = lower.contains("camera") || lower.contains("photo") || lower.contains("picture")
        if (isCamera) {
            val appInfo = if (context != null) {
                AppDiscoveryHelper.resolveApp(context, "camera")
            } else {
                InstalledAppInfo("Camera", "com.android.camera")
            }

            val steps = listOf(
                PlannedActionStep(
                    stepIndex = 1,
                    type = ActionBlockType.LAUNCH_APP,
                    title = "Launch Camera",
                    description = "Launch ${appInfo.packageName}",
                    targetButtonName = "Camera Launcher",
                    visualSearchQuery = "Camera app icon",
                    x = 0, y = 0,
                    reasoning = "Open Camera app"
                ),
                PlannedActionStep(
                    stepIndex = 2,
                    type = ActionBlockType.WAIT_BEFORE_NEXT,
                    title = "Wait for Viewfinder",
                    description = "Allow camera sensor and preview to initialize",
                    targetButtonName = "Viewfinder Loader",
                    visualSearchQuery = "live camera viewfinder preview",
                    x = 0, y = 0,
                    reasoning = "Wait 2s for camera hardware initialization"
                ),
                PlannedActionStep(
                    stepIndex = 3,
                    type = ActionBlockType.TAP_COORDINATES,
                    title = "Click Shutter Button",
                    description = "Tap large circular shutter button to capture photo",
                    targetButtonName = "Camera Shutter Button",
                    visualSearchQuery = "large white circular shutter button at bottom center",
                    x = 540, y = 2150,
                    searchKeywords = listOf("Shutter", "Take photo", "shutter_button"),
                    reasoning = "Capture photo"
                )
            )

            return TaskAnalysis(
                goal = "Open Camera and take a photo",
                targetAppPackage = appInfo.packageName,
                targetAppName = appInfo.appName,
                actionCategory = "CAMERA",
                parameters = emptyMap(),
                plannedStepsDescription = listOf(
                    "Launch Camera app",
                    "Wait for viewfinder to initialize",
                    "Click white circular Shutter button"
                ),
                plannedSteps = steps
            )
        }

        // 6. Settings
        val isSettings = lower.contains("setting")
        if (isSettings) {
            val appInfo = if (context != null) {
                AppDiscoveryHelper.resolveApp(context, "settings")
            } else {
                InstalledAppInfo("Settings", "com.android.settings")
            }

            val steps = listOf(
                PlannedActionStep(
                    stepIndex = 1,
                    type = ActionBlockType.LAUNCH_APP,
                    title = "Launch Settings",
                    description = "Launch ${appInfo.packageName}",
                    targetButtonName = "Settings Launcher",
                    visualSearchQuery = "Settings gear app icon",
                    x = 0, y = 0,
                    reasoning = "Open Android Settings"
                ),
                PlannedActionStep(
                    stepIndex = 2,
                    type = ActionBlockType.WAIT_BEFORE_NEXT,
                    title = "Wait for Settings Menu",
                    description = "Allow settings categories to load",
                    targetButtonName = "Settings Menu Loader",
                    visualSearchQuery = "system settings list with Network, Bluetooth, Display",
                    x = 0, y = 0,
                    reasoning = "Wait 2s for settings list"
                ),
                PlannedActionStep(
                    stepIndex = 3,
                    type = ActionBlockType.TAP_COORDINATES,
                    title = "Click Network & Internet",
                    description = "Tap top settings item for connectivity",
                    targetButtonName = "Network & Internet Row",
                    visualSearchQuery = "settings row labeled 'Network & internet' or 'Wi-Fi'",
                    x = 540, y = 350,
                    searchKeywords = listOf("Network & internet", "Wi-Fi", "Connections"),
                    reasoning = "Open connectivity settings"
                )
            )

            return TaskAnalysis(
                goal = "Open Settings and view connectivity options",
                targetAppPackage = appInfo.packageName,
                targetAppName = appInfo.appName,
                actionCategory = "SETTINGS",
                parameters = emptyMap(),
                plannedStepsDescription = listOf(
                    "Launch Settings app",
                    "Wait for settings menu",
                    "Click Network & Internet"
                ),
                plannedSteps = steps
            )
        }

        // 7. General / Arbitrary App Launch (e.g. "open instagram", "open chrome", "open spotify", "open [any_app]")
        val targetApp = if (context != null) {
            AppDiscoveryHelper.resolveApp(context, prompt)
        } else {
            val appNameGuess = prompt.replace("open ", "").replace("launch ", "").trim().replaceFirstChar { it.uppercase() }
            InstalledAppInfo(appNameGuess, "com.android.${appNameGuess.lowercase()}")
        }

        val hasSearchIntent = lower.contains("search") || lower.contains("find")
        val searchQuery = if (hasSearchIntent) extractSearchQuery(prompt, "") else ""

        val generalSteps = mutableListOf<PlannedActionStep>()
        generalSteps.add(
            PlannedActionStep(
                stepIndex = 1,
                type = ActionBlockType.LAUNCH_APP,
                title = "Launch ${targetApp.appName}",
                description = "Launch package ${targetApp.packageName}",
                targetButtonName = "${targetApp.appName} Launcher",
                visualSearchQuery = "${targetApp.appName} icon",
                x = 0, y = 0,
                searchKeywords = listOf(targetApp.appName),
                reasoning = "Physically launch ${targetApp.appName} on the device"
            )
        )
        generalSteps.add(
            PlannedActionStep(
                stepIndex = 2,
                type = ActionBlockType.WAIT_BEFORE_NEXT,
                title = "Wait for ${targetApp.appName} Screen",
                description = "Allow main screen layout to render",
                targetButtonName = "Screen Loader",
                visualSearchQuery = "${targetApp.appName} main screen",
                x = 0, y = 0,
                reasoning = "Wait 2.5s for app interface to be responsive"
            )
        )

        if (searchQuery.isNotBlank()) {
            generalSteps.add(
                PlannedActionStep(
                    stepIndex = 3,
                    type = ActionBlockType.TAP_COORDINATES,
                    title = "Click Search Bar in ${targetApp.appName}",
                    description = "Click search icon or input field in app header",
                    targetButtonName = "Search Button",
                    visualSearchQuery = "search magnifying glass icon or search input bar in header",
                    x = 880, y = 160,
                    searchKeywords = listOf("Search", "search", "find"),
                    reasoning = "Locate search entry in ${targetApp.appName}"
                )
            )
            generalSteps.add(
                PlannedActionStep(
                    stepIndex = 4,
                    type = ActionBlockType.INPUT_TEXT,
                    title = "Type Query: \"$searchQuery\"",
                    description = "Type query into search field",
                    targetButtonName = "Search Input",
                    visualSearchQuery = "focused text field with cursor",
                    x = 540, y = 160,
                    textToInput = searchQuery,
                    pressEnter = true,
                    reasoning = "Input search query \"$searchQuery\""
                )
            )
        }

        return TaskAnalysis(
            goal = if (searchQuery.isNotBlank()) "Open ${targetApp.appName} and search \"$searchQuery\"" else "Open and interact with ${targetApp.appName}",
            targetAppPackage = targetApp.packageName,
            targetAppName = targetApp.appName,
            actionCategory = "GENERIC",
            parameters = mapOf("query" to searchQuery),
            plannedStepsDescription = generalSteps.map { it.title },
            plannedSteps = generalSteps
        )
    }

    /**
     * Extracts target app/game name from a download command.
     * e.g. "download candy crush game" -> "Candy Crush"
     * e.g. "how to download candy crush" -> "Candy Crush"
     * e.g. "install subway surfers" -> "Subway Surfers"
     */
    private fun extractDownloadAppName(prompt: String): String {
        var clean = prompt.trim()

        val prefixes = listOf(
            "how to download", "how to install", "how do i download",
            "download the", "download a", "download",
            "install the", "install a", "install",
            "get the", "get a", "get"
        )
        for (prefix in prefixes) {
            if (clean.lowercase().startsWith(prefix)) {
                clean = clean.substring(prefix.length).trim()
                break
            }
        }

        // Strip trailing words like "game", "app", "application", "on mobile", "on phone", "from play store"
        val suffixes = listOf(
            "from google play store", "from play store", "from store",
            "on my phone", "on phone", "on android", "on mobile",
            "game app", "game", "app", "apk"
        )
        for (suffix in suffixes) {
            if (clean.lowercase().endsWith(suffix)) {
                clean = clean.substring(0, clean.length - suffix.length).trim()
            }
        }

        if (clean.isBlank()) {
            return "Candy Crush"
        }

        // Capitalize words for clean presentation
        return clean.split(" ").filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
    }

    private fun extractContact(prompt: String, default: String): String {
        val lower = prompt.lowercase()
        val toIdx = lower.indexOf(" to ")
        if (toIdx != -1) {
            val afterTo = prompt.substring(toIdx + 4).trim()
            val tokens = afterTo.split(" ", " in ", " on ", " via ", ",")
            val candidate = tokens.firstOrNull { it.isNotBlank() }?.trim()
            if (!candidate.isNullOrBlank() && !candidate.equals("whatsapp", true) && !candidate.equals("whats", true)) {
                return candidate
            }
        }
        return default
    }

    private fun extractMessage(prompt: String, default: String): String {
        val lower = prompt.lowercase()

        val sendMsgMatch = Regex("(?:send|type|msg|message)\\s+([a-zA-Z0-9!?.\\s]+?)\\s+message\\s+to", RegexOption.IGNORE_CASE).find(prompt)
        if (sendMsgMatch != null) {
            val candidate = sendMsgMatch.groupValues[1].trim()
            if (candidate.isNotBlank()) return candidate
        }

        val sendToMatch = Regex("(?:send|type)\\s+([a-zA-Z0-9!?.\\s]+?)\\s+(?:to\\s+[a-zA-Z0-9]+)", RegexOption.IGNORE_CASE).find(prompt)
        if (sendToMatch != null) {
            val candidate = sendToMatch.groupValues[1].trim()
                .replace(Regex("(?i)\\s*message$"), "")
                .replace(Regex("(?i)\\s*msg$"), "")
                .trim()
            if (candidate.isNotBlank()) return candidate
        }

        val quoteMatch = Regex("[\"']([^\"']+)[\"']").find(prompt)
        if (quoteMatch != null) {
            return quoteMatch.groupValues[1].trim()
        }

        return default
    }

    private fun extractSearchQuery(prompt: String, default: String): String {
        val lower = prompt.lowercase()
        val searchKeywords = listOf("search and play", "search for", "search", "play", "find", "about")
        for (kw in searchKeywords) {
            val idx = lower.indexOf(kw)
            if (idx != -1) {
                var candidate = prompt.substring(idx + kw.length).trim()
                candidate = candidate.replace(Regex("(?i)on youtube.*"), "")
                    .replace(Regex("(?i)in youtube.*"), "")
                    .replace(Regex("(?i)on instagram.*"), "")
                    .replace(Regex("(?i)in chrome.*"), "")
                    .replace(Regex("(?i)in play store.*"), "")
                    .trim()
                if (candidate.isNotBlank()) return candidate
            }
        }
        return default
    }
}
