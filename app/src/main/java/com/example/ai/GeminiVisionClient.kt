package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.model.ActionBlockType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class AiVisionStepDecision(
    val actionType: String,
    val x: Int? = null,
    val y: Int? = null,
    val label: String = "",
    val text: String = "",
    val pressEnter: Boolean = true,
    val reasoning: String = "",
    val isTaskComplete: Boolean = false
)

class GeminiVisionClient {

    companion object {
        private const val TAG = "GeminiVisionClient"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
        // Use gemini-3.5-flash as mandated by gemini-api guidelines
        const val DEFAULT_MODEL = "gemini-3.5-flash"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Searches a screenshot for the specific target of a planned step,
     * returning high-precision (X, Y) pixel coordinates.
     */
    suspend fun locateTargetCoordinates(
        apiKey: String,
        screenshotBitmap: Bitmap?,
        step: PlannedActionStep,
        screenWidth: Int = 1080,
        screenHeight: Int = 2400
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || screenshotBitmap == null) {
            return@withContext VisionCoordinateDetector.locateTargetOnScreen(
                step = step,
                screenWidth = screenWidth,
                screenHeight = screenHeight
            )
        }

        try {
            val systemInstruction = """
                You are a pixel-accurate Android UI Computer Vision Detector.
                Given a screenshot of an Android mobile device screen (resolution ${screenWidth}x${screenHeight}),
                locate the target UI element: "${step.targetButtonName}".
                Visual description: "${step.visualSearchQuery}".
                
                Return ONLY valid JSON matching this exact structure:
                {
                   "x": 540,
                   "y": 160,
                   "confidence": 0.96,
                   "label": "${step.targetButtonName}"
                }
            """.trimIndent()

            val contentsArray = JSONArray()
            val userContent = JSONObject()
            val partsArray = JSONArray()

            partsArray.put(JSONObject().put("text", "Locate target '${step.targetButtonName}' (${step.visualSearchQuery}) in this screenshot:"))

            val outputStream = ByteArrayOutputStream()
            screenshotBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            val inlineData = JSONObject()
                .put("mimeType", "image/jpeg")
                .put("data", base64Image)
            partsArray.put(JSONObject().put("inlineData", inlineData))

            userContent.put("parts", partsArray)
            contentsArray.put(userContent)

            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.1)
                })
            }

            val request = Request.Builder()
                .url("${BASE_URL}$DEFAULT_MODEL:generateContent?key=$apiKey")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val jsonObject = JSONObject(responseBody)
                val candidates = jsonObject.optJSONArray("candidates")
                val textResponse = candidates?.optJSONObject(0)?.optJSONObject("content")
                    ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: ""

                val cleanJson = textResponse.trim().removeSurrounding("```json", "```").trim()
                val parsed = JSONObject(cleanJson)
                val foundX = parsed.optInt("x", -1)
                val foundY = parsed.optInt("y", -1)
                if (foundX > 0 && foundY > 0) {
                    return@withContext Pair(foundX, foundY)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini vision locateTargetCoordinates error", e)
        }

        // Fallback to high-precision layout detection
        VisionCoordinateDetector.locateTargetOnScreen(
            step = step,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
    }

    /**
     * Dynamic Step Decision from screenshot
     */
    suspend fun analyzeScreenshotAndDecideNextAction(
        apiKey: String,
        modelName: String = DEFAULT_MODEL,
        userPrompt: String,
        stepIndex: Int,
        executedActionsSummary: String,
        screenshotBitmap: Bitmap?,
        screenWidth: Int = 1080,
        screenHeight: Int = 2400
    ): AiVisionStepDecision = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            Log.d(TAG, "No Gemini API Key provided, using built-in vision coordinate detector")
            return@withContext VisionCoordinateDetector.detectNextStep(
                userPrompt = userPrompt,
                stepIndex = stepIndex,
                screenWidth = screenWidth,
                screenHeight = screenHeight
            )
        }

        try {
            val systemInstruction = """
                You are AIMacroDroid, an expert autonomous Android MacroDroid automation builder.
                Target device screen resolution is ${screenWidth}x${screenHeight} (Vivo Y300 5G).
                The user wants to achieve: "$userPrompt".
                Actions already executed in this Macro:
                $executedActionsSummary

                Inspect the provided screenshot and determine the SINGLE NEXT action block to append to the Macro.
                Possible actions:
                - "TAP_COORDINATES": Provide exact (X, Y) pixel coordinates on the ${screenWidth}x${screenHeight} screen and label.
                - "INPUT_TEXT": Provide the text to type and whether to press Enter.
                - "WAIT_BEFORE_NEXT": Provide seconds to wait if UI is loading.
                - "SWIPE_COORDINATES": Provide x1, y1, x2, y2 coordinates if scrolling is required.
                - "TASK_COMPLETE": If the user's objective is achieved (e.g. video is playing, app reached target state).

                Return ONLY valid JSON matching this exact structure:
                {
                   "actionType": "TAP_COORDINATES" | "INPUT_TEXT" | "WAIT_BEFORE_NEXT" | "SWIPE_COORDINATES" | "TASK_COMPLETE",
                   "x": 940,
                   "y": 140,
                   "label": "Play Store Search Bar",
                   "text": "Candy Crush",
                   "pressEnter": true,
                   "reasoning": "Located the search bar at top at X: 540, Y: 160",
                   "isTaskComplete": false
                }
            """.trimIndent()

            val contentsArray = JSONArray()
            val userContent = JSONObject()
            val partsArray = JSONArray()

            partsArray.put(JSONObject().put("text", "User Goal: $userPrompt\nStep: $stepIndex\nAnalyze screenshot and return next action JSON:"))

            if (screenshotBitmap != null) {
                val outputStream = ByteArrayOutputStream()
                screenshotBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
                val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

                val inlineData = JSONObject()
                    .put("mimeType", "image/jpeg")
                    .put("data", base64Image)
                partsArray.put(JSONObject().put("inlineData", inlineData))
            }

            userContent.put("parts", partsArray)
            contentsArray.put(userContent)

            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                })
            }

            val request = Request.Builder()
                .url("${BASE_URL}$DEFAULT_MODEL:generateContent?key=$apiKey")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini API error ${response.code}: $responseBody")
                return@withContext VisionCoordinateDetector.detectNextStep(
                    userPrompt = userPrompt,
                    stepIndex = stepIndex,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight
                )
            }

            val jsonObject = JSONObject(responseBody)
            val candidates = jsonObject.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textResponse = parts?.optJSONObject(0)?.optString("text") ?: ""

            parseDecisionJson(textResponse, userPrompt, stepIndex, screenWidth, screenHeight)
        } catch (e: Exception) {
            Log.e(TAG, "Error in Gemini Vision API call", e)
            VisionCoordinateDetector.detectNextStep(
                userPrompt = userPrompt,
                stepIndex = stepIndex,
                screenWidth = screenWidth,
                screenHeight = screenHeight
            )
        }
    }

    private fun parseDecisionJson(
        jsonString: String,
        userPrompt: String,
        stepIndex: Int,
        screenWidth: Int,
        screenHeight: Int
    ): AiVisionStepDecision {
        return try {
            val cleanJson = jsonString.trim().removeSurrounding("```json", "```").trim()
            val obj = JSONObject(cleanJson)
            AiVisionStepDecision(
                actionType = obj.optString("actionType", "TAP_COORDINATES"),
                x = if (obj.has("x")) obj.optInt("x") else null,
                y = if (obj.has("y")) obj.optInt("y") else null,
                label = obj.optString("label", "Target UI Element"),
                text = obj.optString("text", ""),
                pressEnter = obj.optBoolean("pressEnter", true),
                reasoning = obj.optString("reasoning", "Identified target element on screen"),
                isTaskComplete = obj.optBoolean("isTaskComplete", false)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON response: $jsonString", e)
            VisionCoordinateDetector.detectNextStep(
                userPrompt = userPrompt,
                stepIndex = stepIndex,
                screenWidth = screenWidth,
                screenHeight = screenHeight
            )
        }
    }
}
