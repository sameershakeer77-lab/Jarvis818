package com.example

import com.example.ai.TaskPlanner
import com.example.model.ActionBlockType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testDownloadCandyCrushGameCommandAnalysis() {
        val analysis = TaskPlanner.analyzeCommand("download candy crush game")

        assertEquals("Google Play Store", analysis.targetAppName)
        assertEquals("com.android.vending", analysis.targetAppPackage)
        assertEquals("APP_DOWNLOAD", analysis.actionCategory)
        assertEquals("Candy Crush", analysis.parameters["appName"])

        // Verify planned step sequence has all necessary steps
        val stepTypes = analysis.plannedSteps.map { it.type }
        assertTrue(stepTypes.contains(ActionBlockType.LAUNCH_APP))
        assertTrue(stepTypes.contains(ActionBlockType.TAP_COORDINATES))
        assertTrue(stepTypes.contains(ActionBlockType.INPUT_TEXT))

        // Check Play Store search bar, query typing, and Install button
        val buttonNames = analysis.plannedSteps.map { it.targetButtonName }
        assertTrue(buttonNames.any { it.contains("Search Bar", ignoreCase = true) })
        assertTrue(buttonNames.any { it.contains("Install", ignoreCase = true) })
    }

    @Test
    fun testWhatsAppCommandAnalysis() {
        val analysis = TaskPlanner.analyzeCommand("send hi message to shakeer in whats")

        assertEquals("WhatsApp", analysis.targetAppName)
        assertEquals("com.whatsapp", analysis.targetAppPackage)
        assertEquals("MESSAGING", analysis.actionCategory)
        assertEquals("shakeer", analysis.parameters["contact"])
        assertEquals("hi", analysis.parameters["message"])

        // Verify planned step sequence has all necessary steps
        val stepTypes = analysis.plannedSteps.map { it.type }
        assertTrue(stepTypes.contains(ActionBlockType.LAUNCH_APP))
        assertTrue(stepTypes.contains(ActionBlockType.TAP_COORDINATES))
        assertTrue(stepTypes.contains(ActionBlockType.INPUT_TEXT))

        // Check search button, contact click, text box click, and send button
        val buttonNames = analysis.plannedSteps.map { it.targetButtonName }
        assertTrue(buttonNames.any { it.contains("Search", ignoreCase = true) })
        assertTrue(buttonNames.any { it.contains("shakeer", ignoreCase = true) })
        assertTrue(buttonNames.any { it.contains("Message", ignoreCase = true) })
        assertTrue(buttonNames.any { it.contains("Send", ignoreCase = true) })
    }

    @Test
    fun testYouTubeCommandAnalysis() {
        val analysis = TaskPlanner.analyzeCommand("open youtube and search and play VJ Siddhu Vlogs")
        assertEquals("YouTube", analysis.targetAppName)
        assertEquals("com.google.android.youtube", analysis.targetAppPackage)
        assertEquals("VJ Siddhu Vlogs", analysis.parameters["query"])
    }

    @Test
    fun testCalculatorCommandAnalysis() {
        val analysis = TaskPlanner.analyzeCommand("open calculator and add 50 + 25")
        assertEquals("Calculator", analysis.targetAppName)
        assertEquals("CALCULATOR", analysis.actionCategory)
    }

    @Test
    fun testNonYouTubeAppLaunch() {
        // Must resolve to the requested app and NOT default to YouTube
        val cameraAnalysis = TaskPlanner.analyzeCommand("open camera")
        assertEquals("Camera", cameraAnalysis.targetAppName)

        val settingsAnalysis = TaskPlanner.analyzeCommand("open settings")
        assertEquals("Settings", settingsAnalysis.targetAppName)

        val instagramAnalysis = TaskPlanner.analyzeCommand("open instagram")
        assertEquals("Instagram", instagramAnalysis.targetAppName)
    }
}
