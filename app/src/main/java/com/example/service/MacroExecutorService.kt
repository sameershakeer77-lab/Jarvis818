package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity
import com.example.R
import com.example.util.DeviceDisplayUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class MacroExecutorService : AccessibilityService() {

    companion object {
        private const val TAG = "MacroExecutorService"

        @Volatile
        var instance: MacroExecutorService? = null
            private set

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        fun isRunning(): Boolean = instance != null
    }

    private var floatingOverlayView: View? = null
    private var windowManager: WindowManager? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        Log.i(TAG, "AIMacroDroid Native Accessibility Service connected on device")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Can monitor target application activity and window state
    }

    override fun onInterrupt() {
        Log.w(TAG, "AIMacroDroid Accessibility Service interrupted")
    }

    override fun onDestroy() {
        hideFloatingOverlay()
        if (instance == this) {
            instance = null
            _isServiceActive.value = false
        }
        super.onDestroy()
        Log.i(TAG, "AIMacroDroid Accessibility Service destroyed")
    }

    /**
     * Executes real tap gesture.
     */
    fun dispatchTap(x: Float, y: Float, onComplete: ((Boolean) -> Unit)? = null) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 80)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                onComplete?.invoke(true)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                onComplete?.invoke(false)
            }
        }, null)
    }

    /**
     * Executes real tap gesture asynchronously with coroutine continuation.
     */
    suspend fun dispatchTapSuspend(x: Float, y: Float): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val path = Path().apply { moveTo(x, y) }
            val stroke = GestureDescription.StrokeDescription(path, 0, 80)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()

            val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d(TAG, "Real device tap executed successfully at ($x, $y)")
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.w(TAG, "Real device tap cancelled at ($x, $y)")
                    if (continuation.isActive) continuation.resume(false)
                }
            }, null)

            if (!dispatched) {
                Log.e(TAG, "Failed to dispatch real tap gesture at ($x, $y)")
                if (continuation.isActive) continuation.resume(false)
            }
        }
    }

    /**
     * Executes real swipe gesture asynchronously with coroutine continuation.
     */
    suspend fun dispatchSwipeSuspend(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        durationMs: Long = 350L
    ): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val path = Path().apply {
                moveTo(x1, y1)
                lineTo(x2, y2)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(100L))
            val gesture = GestureDescription.Builder().addStroke(stroke).build()

            val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d(TAG, "Real device swipe completed from ($x1, $y1) to ($x2, $y2)")
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.w(TAG, "Real device swipe cancelled")
                    if (continuation.isActive) continuation.resume(false)
                }
            }, null)

            if (!dispatched) {
                if (continuation.isActive) continuation.resume(false)
            }
        }
    }

    /**
     * Injects text on the real device into the currently active or focused input field.
     * Runs on Dispatchers.Main to ensure synchronization with the Accessibility framework.
     */
    suspend fun inputTextOnDevice(text: String, pressEnter: Boolean = true): Boolean = withContext(Dispatchers.Main) {
        val rootNode = try { rootInActiveWindow } catch (e: Exception) { null } ?: return@withContext false
        try {
            var targetNode = try { rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) } catch (e: Exception) { null }
            if (targetNode == null) {
                targetNode = findFirstEditableNode(rootNode)
            }

            if (targetNode != null) {
                try {
                    targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                } catch (e: Exception) {
                    Log.w(TAG, "Focus action failed", e)
                }
                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                var result = try {
                    targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                } catch (e: Exception) {
                    false
                }

                // If set text didn't work, try clipboard paste
                if (!result) {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("macro_input", text))
                    result = try {
                        targetNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                    } catch (e: Exception) {
                        false
                    }
                }

                if (pressEnter) {
                    // Try to trigger search / submit button action
                    findAndClickSearchOrSubmit(rootNode)
                    // Also dispatch tap on keyboard search key (bottom-right corner)
                    val deviceSize = DeviceDisplayUtils.getRealDisplaySize(this@MacroExecutorService)
                    val enterX = deviceSize.width * 0.91f
                    val enterY = deviceSize.height * 0.94f
                    dispatchTap(enterX, enterY)
                }

                return@withContext true
            } else {
                Log.w(TAG, "No editable field found in active window")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error injecting text on device", e)
            return@withContext false
        }
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val className = node.className?.toString() ?: ""
        if (node.isEditable || className.contains("EditText", ignoreCase = true)) return node
        for (i in 0 until node.childCount) {
            val child = try { node.getChild(i) } catch (e: Exception) { null } ?: continue
            val editable = findFirstEditableNode(child)
            if (editable != null) return editable
        }
        return null
    }

    /**
     * Searches active UI hierarchy for elements matching keywords (text, content description, ID),
     * clicks the matching element, and returns its exact screen center coordinates (X, Y).
     * Runs on Dispatchers.Main to avoid race conditions with accessibility event processing.
     */
    suspend fun findAndClickNodeByKeywords(keywords: List<String>): Pair<Int, Int>? = withContext(Dispatchers.Main) {
        val root = try { rootInActiveWindow } catch (e: Exception) { null } ?: return@withContext null
        val rect = android.graphics.Rect()
        try {
            val matchingNode = searchNodeRecursively(root, keywords)
            if (matchingNode != null) {
                matchingNode.getBoundsInScreen(rect)
                val cx = rect.centerX().toFloat()
                val cy = rect.centerY().toFloat()

                // Try accessibility click on node or ancestors
                var clicked = try {
                    matchingNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                } catch (e: Exception) {
                    false
                }
                if (!clicked) {
                    val p1 = try { matchingNode.parent } catch (e: Exception) { null }
                    clicked = try {
                        p1?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
                    } catch (e: Exception) {
                        false
                    }
                }

                // If accessibility click was not handled or node is touch-only, dispatch physical gesture tap at center
                if (!clicked && cx > 0 && cy > 0) {
                    dispatchTap(cx, cy)
                    clicked = true
                }

                Log.d(TAG, "Found & clicked node matching $keywords at ($cx, $cy) -> $clicked")
                return@withContext cx.toInt() to cy.toInt()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in findAndClickNodeByKeywords", e)
        }
        return@withContext null
    }

    private fun searchNodeRecursively(node: AccessibilityNodeInfo, keywords: List<String>): AccessibilityNodeInfo? {
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val id = node.viewIdResourceName?.lowercase() ?: ""

        for (kw in keywords) {
            val k = kw.lowercase()
            if (text.contains(k) || desc.contains(k) || id.contains(k)) {
                return node
            }
        }

        for (i in 0 until node.childCount) {
            val child = try { node.getChild(i) } catch (e: Exception) { null } ?: continue
            val found = searchNodeRecursively(child, keywords)
            if (found != null) return found
        }
        return null
    }

    private fun findAndClickSearchOrSubmit(root: AccessibilityNodeInfo) {
        val keywords = listOf("search", "submit", "go", "enter", "find")
        for (kw in keywords) {
            val nodes = try { root.findAccessibilityNodeInfosByText(kw) } catch (e: Exception) { null } ?: continue
            for (node in nodes) {
                if (node.isClickable) {
                    try {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed clicking submit node", e)
                    }
                    return
                }
            }
        }
    }

    /**
     * Launches external app package on the real device.
     */
    fun launchTargetApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                startActivity(intent)
                true
            } else {
                Log.w(TAG, "Target app package not found: $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package $packageName", e)
            false
        }
    }

    /**
     * Captures the live device screenshot bitmap via Accessibility API.
     */
    suspend fun captureScreenSuspend(): Bitmap? = withContext(Dispatchers.Main) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            suspendCancellableCoroutine { continuation ->
                try {
                    takeScreenshot(
                        Display.DEFAULT_DISPLAY,
                        applicationContext.mainExecutor,
                        object : TakeScreenshotCallback {
                            override fun onSuccess(screenshotResult: ScreenshotResult) {
                                val buffer = screenshotResult.hardwareBuffer
                                val colorSpace = screenshotResult.colorSpace
                                val bitmap = Bitmap.wrapHardwareBuffer(buffer, colorSpace)?.copy(
                                    Bitmap.Config.ARGB_8888,
                                    false
                                )
                                buffer.close()
                                if (continuation.isActive) continuation.resume(bitmap)
                            }

                            override fun onFailure(errorCode: Int) {
                                Log.e(TAG, "Live on-device screenshot failed code: $errorCode")
                                if (continuation.isActive) continuation.resume(null)
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Exception capturing screen on device", e)
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        } else {
            null
        }
    }

    /**
     * Displays a compact floating overlay pill on top of the phone screen while other apps
     * are being automated so the user has full real-time feedback and control.
     */
    fun showFloatingOverlay(stepText: String, onStop: () -> Unit) {
        mainHandler.post {
            try {
                if (floatingOverlayView != null) {
                    val layout = floatingOverlayView as? LinearLayout
                    val textView = layout?.getChildAt(0) as? TextView
                    textView?.text = "🤖 $stepText"
                    return@post
                }
                val wm = windowManager ?: (getSystemService(Context.WINDOW_SERVICE) as? WindowManager) ?: return@post
                windowManager = wm

                val layout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    val bg = GradientDrawable().apply {
                        setColor(Color.parseColor("#E60F172A"))
                        cornerRadius = 40f
                        setStroke(3, Color.parseColor("#06B6D4"))
                    }
                    background = bg
                    setPadding(30, 16, 30, 16)
                }

                val titleText = TextView(this).apply {
                    text = "🤖 $stepText"
                    setTextColor(Color.WHITE)
                    textSize = 12f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }

                val stopBtn = Button(this).apply {
                    text = "■ STOP"
                    setTextColor(Color.parseColor("#EF4444"))
                    textSize = 11f
                    setBackgroundColor(Color.TRANSPARENT)
                    setOnClickListener {
                        onStop()
                        hideFloatingOverlay()
                    }
                }

                val openAppBtn = Button(this).apply {
                    text = "APP"
                    setTextColor(Color.parseColor("#38BDF8"))
                    textSize = 11f
                    setBackgroundColor(Color.TRANSPARENT)
                    setOnClickListener {
                        val intent = Intent(this@MacroExecutorService, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                        startActivity(intent)
                    }
                }

                layout.addView(titleText)
                layout.addView(openAppBtn)
                layout.addView(stopBtn)

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = 80
                }

                wm.addView(layout, params)
                floatingOverlayView = layout
            } catch (e: Exception) {
                Log.e(TAG, "Error displaying floating overlay", e)
            }
        }
    }

    fun updateFloatingOverlayText(text: String) {
        mainHandler.post {
            try {
                val layout = floatingOverlayView as? LinearLayout ?: return@post
                val textView = layout.getChildAt(0) as? TextView
                textView?.text = "🤖 $text"
            } catch (e: Exception) {
                Log.e(TAG, "Error updating overlay text", e)
            }
        }
    }

    fun hideFloatingOverlay() {
        mainHandler.post {
            try {
                floatingOverlayView?.let {
                    windowManager?.removeView(it)
                    floatingOverlayView = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding floating overlay", e)
            }
        }
    }
}
