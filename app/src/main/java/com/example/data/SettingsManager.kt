package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("aimacrodroid_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_GEMINI_KEY = "gemini_api_key"
        const val KEY_GROQ_KEY = "groq_api_key"
        const val KEY_AI_MODEL = "ai_model"
        const val KEY_RESOLUTION = "screen_resolution"
        const val KEY_SIMULATION_MODE = "simulation_mode"
        const val KEY_STEP_DELAY_MS = "step_delay_ms"

        const val DEFAULT_MODEL = "gemini-2.5-flash"
        const val DEFAULT_RESOLUTION = "1080x2400 (Vivo Y300 5G)"
        const val DEFAULT_DELAY_MS = 1500L
    }

    private val _geminiApiKey = MutableStateFlow(loadGeminiKey())
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _groqApiKey = MutableStateFlow(prefs.getString(KEY_GROQ_KEY, "") ?: "")
    val groqApiKey: StateFlow<String> = _groqApiKey.asStateFlow()

    private val _aiModel = MutableStateFlow(prefs.getString(KEY_AI_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL)
    val aiModel: StateFlow<String> = _aiModel.asStateFlow()

    private val _resolution = MutableStateFlow(prefs.getString(KEY_RESOLUTION, DEFAULT_RESOLUTION) ?: DEFAULT_RESOLUTION)
    val resolution: StateFlow<String> = _resolution.asStateFlow()

    private val _simulationMode = MutableStateFlow(prefs.getBoolean(KEY_SIMULATION_MODE, false))
    val simulationMode: StateFlow<Boolean> = _simulationMode.asStateFlow()

    private val _stepDelayMs = MutableStateFlow(prefs.getLong(KEY_STEP_DELAY_MS, DEFAULT_DELAY_MS))
    val stepDelayMs: StateFlow<Long> = _stepDelayMs.asStateFlow()

    private fun loadGeminiKey(): String {
        val saved = prefs.getString(KEY_GEMINI_KEY, "") ?: ""
        if (saved.isNotBlank()) return saved
        return try {
            if (BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY") BuildConfig.GEMINI_API_KEY else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun setGeminiApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_KEY, key.trim()).apply()
        _geminiApiKey.value = key.trim()
    }

    fun setGroqApiKey(key: String) {
        prefs.edit().putString(KEY_GROQ_KEY, key.trim()).apply()
        _groqApiKey.value = key.trim()
    }

    fun setAiModel(model: String) {
        prefs.edit().putString(KEY_AI_MODEL, model).apply()
        _aiModel.value = model
    }

    fun setResolution(res: String) {
        prefs.edit().putString(KEY_RESOLUTION, res).apply()
        _resolution.value = res
    }

    fun setSimulationMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SIMULATION_MODE, enabled).apply()
        _simulationMode.value = enabled
    }

    fun setStepDelayMs(delayMs: Long) {
        prefs.edit().putLong(KEY_STEP_DELAY_MS, delayMs).apply()
        _stepDelayMs.value = delayMs
    }
}
