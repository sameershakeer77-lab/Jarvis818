package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.VisionCoordinateDetector
import com.example.model.ActionBlock
import com.example.model.ActionBlockType
import com.example.model.BlockStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AIMacroDroid", appName)
    }

    @Test
    fun `test ActionBlock parameters and summary`() {
        val block = ActionBlock(
            stepNumber = 1,
            type = ActionBlockType.TAP_COORDINATES,
            title = "Tap Search",
            description = "Tap search button at (940, 140)",
            params = mapOf("x" to "940", "y" to "140", "label" to "Search Button"),
            status = BlockStatus.COMPLETED
        )

        assertEquals(940, block.targetX)
        assertEquals(140, block.targetY)
        assertEquals("Search Button", block.label)
        assertEquals(BlockStatus.COMPLETED, block.status)
    }

    @Test
    fun `test VisionCoordinateDetector YouTube flow`() {
        val step1 = VisionCoordinateDetector.detectNextStep(
            userPrompt = "Open YouTube and search and play VJ Siddhu Vlogs",
            stepIndex = 1,
            screenWidth = 1080,
            screenHeight = 2400
        )
        assertEquals("TAP_COORDINATES", step1.actionType)
        assertNotNull(step1.x)
        assertNotNull(step1.y)

        val step2 = VisionCoordinateDetector.detectNextStep(
            userPrompt = "Open YouTube and search and play VJ Siddhu Vlogs",
            stepIndex = 2,
            screenWidth = 1080,
            screenHeight = 2400
        )
        assertEquals("INPUT_TEXT", step2.actionType)
        assertEquals("VJ Siddhu Vlogs", step2.text)
    }
}
