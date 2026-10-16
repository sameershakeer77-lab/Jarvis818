package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ActionBlock
import com.example.model.ActionBlockType
import com.example.model.BlockStatus
import com.example.model.Macro
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class MacroRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("aimacrodroid_saved_macros", Context.MODE_PRIVATE)

    private val _savedMacros = MutableStateFlow<List<Macro>>(emptyList())
    val savedMacros: StateFlow<List<Macro>> = _savedMacros.asStateFlow()

    init {
        loadMacros()
    }

    private fun loadMacros() {
        val jsonStr = prefs.getString("macros_list_json", null)
        if (jsonStr.isNullOrBlank()) {
            val defaults = createSampleMacros()
            _savedMacros.value = defaults
            persistMacros(defaults)
        } else {
            try {
                val list = deserializeMacros(jsonStr)
                if (list.isEmpty()) {
                    val defaults = createSampleMacros()
                    _savedMacros.value = defaults
                    persistMacros(defaults)
                } else {
                    _savedMacros.value = list
                }
            } catch (e: Exception) {
                val defaults = createSampleMacros()
                _savedMacros.value = defaults
            }
        }
    }

    fun saveMacro(macro: Macro) {
        val current = _savedMacros.value.toMutableList()
        val index = current.indexOfFirst { it.id == macro.id }
        if (index >= 0) {
            current[index] = macro
        } else {
            current.add(0, macro)
        }
        _savedMacros.value = current
        persistMacros(current)
    }

    fun deleteMacro(macroId: String) {
        val updated = _savedMacros.value.filter { it.id != macroId }
        _savedMacros.value = updated
        persistMacros(updated)
    }

    fun duplicateMacro(macro: Macro): Macro {
        val newMacro = macro.copy(
            id = java.util.UUID.randomUUID().toString(),
            title = "${macro.title} (Copy)",
            createdAt = System.currentTimeMillis(),
            executionCount = 0,
            lastExecutedAt = null
        )
        saveMacro(newMacro)
        return newMacro
    }

    fun updateActionBlock(macroId: String, updatedBlock: ActionBlock) {
        val macro = _savedMacros.value.firstOrNull { it.id == macroId } ?: return
        val updatedActions = macro.actions.map { block ->
            if (block.id == updatedBlock.id) updatedBlock else block
        }
        saveMacro(macro.copy(actions = updatedActions))
    }

    private fun persistMacros(list: List<Macro>) {
        val jsonArray = JSONArray()
        for (m in list) {
            val mObj = JSONObject().apply {
                put("id", m.id)
                put("title", m.title)
                put("trigger", m.trigger)
                put("sourcePrompt", m.sourcePrompt)
                put("createdAt", m.createdAt)
                put("isCompleted", m.isCompleted)
                put("targetResolution", m.targetResolution)
                put("executionCount", m.executionCount)
                if (m.lastExecutedAt != null) put("lastExecutedAt", m.lastExecutedAt)

                val actionsArr = JSONArray()
                for (a in m.actions) {
                    val aObj = JSONObject().apply {
                        put("id", a.id)
                        put("stepNumber", a.stepNumber)
                        put("type", a.type.name)
                        put("title", a.title)
                        put("description", a.description)
                        put("status", a.status.name)
                        if (a.executionTimeMs != null) put("executionTimeMs", a.executionTimeMs)
                        if (a.resultMessage != null) put("resultMessage", a.resultMessage)

                        val pObj = JSONObject()
                        for ((k, v) in a.params) {
                            pObj.put(k, v)
                        }
                        put("params", pObj)
                    }
                    actionsArr.put(aObj)
                }
                put("actions", actionsArr)
            }
            jsonArray.put(mObj)
        }
        prefs.edit().putString("macros_list_json", jsonArray.toString()).apply()
    }

    private fun deserializeMacros(jsonStr: String): List<Macro> {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<Macro>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val actionsArray = obj.getJSONArray("actions")
            val actions = mutableListOf<ActionBlock>()
            for (j in 0 until actionsArray.length()) {
                val aObj = actionsArray.getJSONObject(j)
                val paramsMap = mutableMapOf<String, String>()
                val pObj = aObj.optJSONObject("params")
                if (pObj != null) {
                    val keys = pObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        paramsMap[key] = pObj.optString(key)
                    }
                }
                actions.add(
                    ActionBlock(
                        id = aObj.getString("id"),
                        stepNumber = aObj.optInt("stepNumber", j + 1),
                        type = ActionBlockType.fromString(aObj.getString("type")),
                        title = aObj.getString("title"),
                        description = aObj.optString("description", ""),
                        params = paramsMap,
                        status = try {
                            BlockStatus.valueOf(aObj.optString("status", BlockStatus.COMPLETED.name))
                        } catch (e: Exception) {
                            BlockStatus.COMPLETED
                        },
                        executionTimeMs = if (aObj.has("executionTimeMs")) aObj.getLong("executionTimeMs") else null,
                        resultMessage = aObj.optString("resultMessage", null)
                    )
                )
            }

            list.add(
                Macro(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    trigger = obj.optString("trigger", "Voice / Prompt Trigger"),
                    sourcePrompt = obj.optString("sourcePrompt", ""),
                    actions = actions,
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    isCompleted = obj.optBoolean("isCompleted", true),
                    targetResolution = obj.optString("targetResolution", "1080x2400 (Vivo Y300 5G)"),
                    executionCount = obj.optInt("executionCount", 1),
                    lastExecutedAt = if (obj.has("lastExecutedAt")) obj.getLong("lastExecutedAt") else null
                )
            )
        }
        return list
    }

    private fun createSampleMacros(): List<Macro> {
        return listOf(
            Macro(
                id = "macro-youtube-vjsiddhu",
                title = "YouTube: Search & Play VJ Siddhu Vlogs",
                trigger = "AI Prompt: Open YouTube and search and play VJ Siddhu Vlogs",
                sourcePrompt = "Open YouTube and search and play VJ Siddhu Vlogs",
                targetResolution = "1080x2400 (Vivo Y300 5G)",
                isCompleted = true,
                executionCount = 3,
                lastExecutedAt = System.currentTimeMillis() - 3600000L,
                actions = listOf(
                    ActionBlock(
                        id = "act-1",
                        stepNumber = 1,
                        type = ActionBlockType.LAUNCH_APP,
                        title = "Launch YouTube",
                        description = "Launch app package com.google.android.youtube",
                        params = mapOf("packageName" to "com.google.android.youtube", "appName" to "YouTube"),
                        status = BlockStatus.COMPLETED,
                        executionTimeMs = 120L
                    ),
                    ActionBlock(
                        id = "act-2",
                        stepNumber = 2,
                        type = ActionBlockType.WAIT_BEFORE_NEXT,
                        title = "Wait for YouTube Load",
                        description = "Wait 3 seconds for UI hierarchy to render",
                        params = mapOf("seconds" to "3"),
                        status = BlockStatus.COMPLETED,
                        executionTimeMs = 3000L
                    ),
                    ActionBlock(
                        id = "act-3",
                        stepNumber = 3,
                        type = ActionBlockType.TAKE_SCREENSHOT_AND_ANALYZE,
                        title = "Capture 1080x2400 Screen",
                        description = "Take screenshot and run Vision AI coordinate analysis",
                        params = mapOf("resolution" to "1080x2400 (Vivo Y300 5G)"),
                        status = BlockStatus.COMPLETED,
                        executionTimeMs = 450L
                    ),
                    ActionBlock(
                        id = "act-4",
                        stepNumber = 4,
                        type = ActionBlockType.TAP_COORDINATES,
                        title = "Tap Search Icon",
                        description = "Tap search icon at (X: 940, Y: 140)",
                        params = mapOf("x" to "940", "y" to "140", "label" to "YouTube Search Button"),
                        status = BlockStatus.COMPLETED,
                        executionTimeMs = 80L
                    ),
                    ActionBlock(
                        id = "act-5",
                        stepNumber = 5,
                        type = ActionBlockType.INPUT_TEXT,
                        title = "Input Query",
                        description = "Type query \"VJ Siddhu Vlogs\" and submit",
                        params = mapOf("text" to "VJ Siddhu Vlogs", "pressEnter" to "true"),
                        status = BlockStatus.COMPLETED,
                        executionTimeMs = 150L
                    ),
                    ActionBlock(
                        id = "act-6",
                        stepNumber = 6,
                        type = ActionBlockType.WAIT_BEFORE_NEXT,
                        title = "Wait for Results",
                        description = "Wait 2 seconds for search results",
                        params = mapOf("seconds" to "2"),
                        status = BlockStatus.COMPLETED,
                        executionTimeMs = 2000L
                    ),
                    ActionBlock(
                        id = "act-7",
                        stepNumber = 7,
                        type = ActionBlockType.TAP_COORDINATES,
                        title = "Tap Video Result",
                        description = "Tap video thumbnail at (X: 540, Y: 820)",
                        params = mapOf("x" to "540", "y" to "820", "label" to "Play VJ Siddhu Vlogs Video"),
                        status = BlockStatus.COMPLETED,
                        executionTimeMs = 90L
                    )
                )
            ),
            Macro(
                id = "macro-playstore-telegram",
                title = "Play Store: Search Telegram",
                trigger = "AI Prompt: Open Play Store and search Telegram",
                sourcePrompt = "Open Play Store and search Telegram",
                targetResolution = "1080x2400 (Vivo Y300 5G)",
                isCompleted = true,
                executionCount = 1,
                lastExecutedAt = System.currentTimeMillis() - 86400000L,
                actions = listOf(
                    ActionBlock(
                        id = "ps-1",
                        stepNumber = 1,
                        type = ActionBlockType.LAUNCH_APP,
                        title = "Launch Play Store",
                        description = "Launch app package com.android.vending",
                        params = mapOf("packageName" to "com.android.vending", "appName" to "Google Play Store"),
                        status = BlockStatus.COMPLETED,
                        executionTimeMs = 110L
                    ),
                    ActionBlock(
                        id = "ps-2",
                        stepNumber = 2,
                        type = ActionBlockType.TAP_COORDINATES,
                        title = "Tap Search Bar",
                        description = "Tap top search bar at (X: 540, Y: 160)",
                        params = mapOf("x" to "540", "y" to "160", "label" to "Play Store Search Bar"),
                        status = BlockStatus.COMPLETED,
                        executionTimeMs = 85L
                    ),
                    ActionBlock(
                        id = "ps-3",
                        stepNumber = 3,
                        type = ActionBlockType.INPUT_TEXT,
                        title = "Input \"Telegram\"",
                        description = "Input Telegram query into search field",
                        params = mapOf("text" to "Telegram", "pressEnter" to "true"),
                        status = BlockStatus.COMPLETED,
                        executionTimeMs = 140L
                    )
                )
            )
        )
    }
}
