package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.EngineState
import com.example.engine.MacroEngine
import com.example.model.ActionBlock
import com.example.model.BlockStatus
import com.example.model.LogType
import com.example.service.MacroExecutorService
import com.example.ui.components.ActionBlockCard
import com.example.ui.components.EditBlockDialog
import com.example.ui.components.FlowConnectorArrow
import com.example.ui.components.ScreenPreviewGrid
import com.example.ui.components.TriggerBlockCard
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MacroTriggerRed
import com.example.ui.theme.SuccessGreen

@Composable
fun LiveConstructorScreen(
    engine: MacroEngine,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var promptInput by remember { mutableStateOf("Download Candy Crush game") }
    var showLogs by remember { mutableStateOf(false) }
    var editingBlock by remember { mutableStateOf<ActionBlock?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val isServiceActive by MacroExecutorService.isServiceActive.collectAsState()
    val isDeviceExecuting by engine.isDeviceExecuting.collectAsState()
    val engineState by engine.engineState.collectAsState()
    val activeMacro by engine.activeMacro.collectAsState()
    val taskAnalysis by engine.taskAnalysis.collectAsState()
    val currentCrosshair by engine.currentCrosshair.collectAsState()
    val screenBitmap by engine.currentScreenBitmap.collectAsState()
    val screenStage by engine.screenStageName.collectAsState()
    val logs by engine.logs.collectAsState()
    val stepIndex by engine.currentStepIndex.collectAsState()

    val quickPrompts = listOf(
        "Download Candy Crush game",
        "Send hi message to shakeer in whats",
        "Open YouTube and search and play VJ Siddhu Vlogs",
        "Open Calculator and add 50 + 25",
        "Open Camera and take photo",
        "Open Instagram and search messi",
        "Open Chrome and search weather today",
        "Open Settings"
    )

    val isRunning = engineState == EngineState.BUILDING_AND_EXECUTING || engineState == EngineState.REPLAYING

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp)
    ) {
        // Device Automation Status Banner
        item {
            if (isServiceActive) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, SuccessGreen.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                    color = Color(0xFF062316)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(SuccessGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "ON-DEVICE AUTOMATION ACTIVE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                            Text(
                                text = "Constructed blocks will execute directly inside apps installed on this device",
                                fontSize = 10.sp,
                                color = Color(0xFFA7F3D0)
                            )
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        },
                    color = Color(0xFF261904)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Target: Physical Device Automation",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                            Text(
                                text = "Tap here to enable Accessibility Service so AI can tap & type on your phone",
                                fontSize = 10.sp,
                                color = Color(0xFFFDE68A)
                            )
                        }
                    }
                }
            }
        }

        // Top AI Prompt Bar
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                    .testTag("prompt_input_card"),
                color = Color(0xFF111827)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI MACRO CONSTRUCTOR PROMPT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanGlow,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prompt_text_field"),
                        placeholder = { Text("E.g. Download Candy Crush game") },
                        trailingIcon = {
                            if (promptInput.isNotBlank()) {
                                IconButton(onClick = { promptInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF161F30),
                            unfocusedContainerColor = Color(0xFF161F30),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyanGlow,
                            unfocusedBorderColor = Color(0xFF26354D)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quick Preset Chips
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickPrompts.forEach { qp ->
                            FilterChip(
                                selected = promptInput == qp,
                                onClick = { promptInput = qp },
                                label = {
                                    Text(
                                        text = qp.take(28) + if (qp.length > 28) "..." else "",
                                        fontSize = 11.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color(0xFFCBD5E1)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = Color(0xFF334155),
                                    enabled = true,
                                    selected = promptInput == qp
                                )
                            )
                        }
                    }

                    // Action Controls
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!MacroExecutorService.isRunning()) {
                                    showPermissionDialog = true
                                } else {
                                    engine.startBuildAndExecute(promptInput)
                                }
                            },
                            enabled = !isRunning && promptInput.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("build_and_execute_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricBlue,
                                disabledContainerColor = Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI Constructing...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Build & Execute Macro", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (isRunning) {
                            OutlinedButton(
                                onClick = { engine.stopExecution() },
                                modifier = Modifier.height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MacroTriggerRed),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop")
                            }
                        }
                    }
                }
            }
        }

        // AI PRE-ANALYSIS & ACTION ROADMAP CARD
        taskAnalysis?.let { analysis ->
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, CyanGlow.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    color = Color(0xFF0F1B2B)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = CyanGlow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI TASK ANALYSIS & ACTION PLAN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanGlow,
                                    letterSpacing = 1.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(ElectricBlue.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = analysis.targetAppName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF93C5FD)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = analysis.goal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "DEFINED STEP-BY-STEP ACTIONS:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        analysis.plannedStepsDescription.forEachIndexed { idx, desc ->
                            val currentActionCount = activeMacro?.actions?.size ?: 0
                            val isCompleted = currentActionCount > idx
                            val isCurrent = isRunning && currentActionCount == idx

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(
                                            when {
                                                isCompleted -> SuccessGreen
                                                isCurrent -> CyanGlow
                                                else -> Color(0xFF1E293B)
                                            },
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCompleted) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "${idx + 1}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) Color.Black else Color(0xFF94A3B8)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = desc,
                                    fontSize = 12.sp,
                                    color = when {
                                        isCompleted -> Color(0xFFA7F3D0)
                                        isCurrent -> Color.White
                                        else -> Color(0xFF94A3B8)
                                    },
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Status Badge
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                when (engineState) {
                                    EngineState.BUILDING_AND_EXECUTING -> CyanGlow
                                    EngineState.COMPLETED -> SuccessGreen
                                    EngineState.ERROR -> MaterialTheme.colorScheme.error
                                    else -> Color.Gray
                                },
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (engineState) {
                            EngineState.BUILDING_AND_EXECUTING -> "AI ENGINE: Step $stepIndex • Building & Executing"
                            EngineState.COMPLETED -> "MACRO COMPLETE: Saved to Tab 2"
                            EngineState.REPLAYING -> "REPLAYING CONSTRUCTED MACRO"
                            EngineState.ERROR -> "ENGINE ERROR"
                            else -> "ENGINE STANDBY: Enter prompt to begin"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Toggle Logs Drawer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    IconButton(
                        onClick = { showLogs = !showLogs },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (showLogs) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle logs",
                            tint = CyanGlow
                        )
                    }
                }
            }
        }

        // Logs Stream Section (collapsible)
        if (showLogs) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp)),
                    color = Color(0xFF030712)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI REASONING & EXECUTION STREAM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanGlow)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        logs.take(6).forEach { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = "[Step ${log.step}] ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (log.type) {
                                        LogType.VISION_AI -> CyanGlow
                                        LogType.BLOCK_ADDED -> ElectricBlue
                                        LogType.EXECUTION_SUCCESS -> SuccessGreen
                                        LogType.EXECUTION_ERROR -> MaterialTheme.colorScheme.error
                                        else -> Color(0xFF94A3B8)
                                    }
                                )
                                Text(
                                    text = log.message,
                                    fontSize = 11.sp,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Device Screen Preview Card (with Coordinate Grid and Red Target Crosshair)
        item {
            Spacer(modifier = Modifier.height(14.dp))
            ScreenPreviewGrid(
                bitmap = screenBitmap,
                stageName = screenStage,
                crosshair = currentCrosshair
            )
        }

        // Section Title: Built-In MacroDroid Flow Builder
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "BUILT-IN MACRODROID FLOW BUILDER",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${activeMacro?.actions?.size ?: 0} Blocks",
                        fontSize = 11.sp,
                        color = CyanGlow,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Flow Builder: Trigger Block at the Top (Red Card)
        item {
            TriggerBlockCard(
                triggerText = activeMacro?.trigger ?: "Voice / Prompt Trigger",
                prompt = activeMacro?.sourcePrompt ?: promptInput
            )
        }

        // Flow Builder: Stack of Action Blocks (Blue Cards) connected by arrows
        val actions = activeMacro?.actions ?: emptyList()
        items(actions) { block ->
            FlowConnectorArrow(active = block.status == BlockStatus.EXECUTING)
            ActionBlockCard(
                block = block,
                onEditClick = { editingBlock = block }
            )
        }

        // In-progress placeholder if constructing next block
        if (isRunning && actions.isEmpty()) {
            item {
                FlowConnectorArrow(active = true)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, CyanGlow.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                    color = Color(0xFF0F172A)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = CyanGlow)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("AI Constructing Action Block #1...", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Determining target package and launch intent...", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }
    }

    // Edit Block Dialog
    editingBlock?.let { block ->
        EditBlockDialog(
            block = block,
            onDismiss = { editingBlock = null },
            onSave = { updated ->
                editingBlock = null
            }
        )
    }

    // Permission Dialog for Real Device Execution
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = null,
                        tint = CyanGlow,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Automate This Device",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 17.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "To allow AIMacroDroid to physically open apps, tap coordinates, type text, and capture screenshots on this device, please enable the AIMacroDroid Accessibility Service in Android Settings.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• Real Device Mode: Automatically executes in apps\n• Simulator Mode: Preview the vision & coordinate steps",
                        color = CyanGlow,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Enable on Device")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        engine.startBuildAndExecute(promptInput)
                    }
                ) {
                    Text("Run Simulator Preview", color = CyanGlow)
                }
            },
            containerColor = Color(0xFF111827),
            shape = RoundedCornerShape(16.dp)
        )
    }
}
