package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsManager
import com.example.service.MacroExecutorService
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MacroActionBlue
import com.example.ui.theme.MacroTriggerRed
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val savedGeminiKey by settingsManager.geminiApiKey.collectAsState()
    val savedGroqKey by settingsManager.groqApiKey.collectAsState()
    val currentModel by settingsManager.aiModel.collectAsState()
    val currentResolution by settingsManager.resolution.collectAsState()
    val isSimulationMode by settingsManager.simulationMode.collectAsState()
    val currentDelayMs by settingsManager.stepDelayMs.collectAsState()

    var geminiKeyInput by remember(savedGeminiKey) { mutableStateOf(savedGeminiKey) }
    var groqKeyInput by remember(savedGroqKey) { mutableStateOf(savedGroqKey) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var showGroqKey by remember { mutableStateOf(false) }

    var modelDropdownExpanded by remember { mutableStateOf(false) }
    var resDropdownExpanded by remember { mutableStateOf(false) }

    val models = listOf("gemini-2.5-flash", "gemini-3.5-flash", "Groq Qwen-2.5-VL")
    val resolutions = listOf(
        "1080x2400 (Vivo Y300 5G)",
        "1080x2340 (Galaxy S24)",
        "1080x2400 (Pixel 8)",
        "1440x3120 (Pixel 8 Pro)",
        "1600x2560 (Tablet)"
    )

    val isServiceActive by MacroExecutorService.isServiceActive.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 90.dp)
    ) {
        // Header
        item {
            Text(
                text = "API KEYS & SETTINGS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Configure Vision AI endpoints, native accessibility service, and screen parameters",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section 1: API Keys
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                    .testTag("api_keys_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("VISION AI API KEYS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Stored securely in private app storage. If left empty, built-in coordinate detection heuristics are used automatically.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Gemini API Key
                    Text("Gemini API Key (Gemini 2.5 Flash / 3.5 Flash)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = geminiKeyInput,
                        onValueChange = { geminiKeyInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_api_key_input"),
                        placeholder = { Text("AIzaSy...") },
                        visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Text(
                                text = if (showGeminiKey) "Hide" else "Show",
                                fontSize = 11.sp,
                                color = CyanGlow,
                                modifier = Modifier
                                    .clickable { showGeminiKey = !showGeminiKey }
                                    .padding(8.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF161F30),
                            unfocusedContainerColor = Color(0xFF161F30),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyanGlow,
                            unfocusedBorderColor = Color(0xFF26354D)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Groq API Key
                    Text("Groq API Key (Qwen-2.5-VL)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = groqKeyInput,
                        onValueChange = { groqKeyInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("groq_api_key_input"),
                        placeholder = { Text("gsk_...") },
                        visualTransformation = if (showGroqKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Text(
                                text = if (showGroqKey) "Hide" else "Show",
                                fontSize = 11.sp,
                                color = CyanGlow,
                                modifier = Modifier
                                    .clickable { showGroqKey = !showGroqKey }
                                    .padding(8.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF161F30),
                            unfocusedContainerColor = Color(0xFF161F30),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyanGlow,
                            unfocusedBorderColor = Color(0xFF26354D)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            settingsManager.setGeminiApiKey(geminiKeyInput)
                            settingsManager.setGroqApiKey(groqKeyInput)
                            Toast.makeText(context, "API Keys saved successfully!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_api_keys_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save API Keys", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section 2: Model & Resolution Selectors
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI MODEL & SCREEN RESOLUTION", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Model Selector Dropdown
                    Text("Vision AI Model", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = modelDropdownExpanded,
                        onExpandedChange = { modelDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = currentModel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF161F30),
                                unfocusedContainerColor = Color(0xFF161F30),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = modelDropdownExpanded,
                            onDismissRequest = { modelDropdownExpanded = false }
                        ) {
                            models.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model) },
                                    onClick = {
                                        settingsManager.setAiModel(model)
                                        modelDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Resolution Selector Dropdown
                    Text("Target Screen Resolution", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = resDropdownExpanded,
                        onExpandedChange = { resDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = currentResolution,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = resDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF161F30),
                                unfocusedContainerColor = Color(0xFF161F30),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = resDropdownExpanded,
                            onDismissRequest = { resDropdownExpanded = false }
                        ) {
                            resolutions.forEach { res ->
                                DropdownMenuItem(
                                    text = { Text(res) },
                                    onClick = {
                                        settingsManager.setResolution(res)
                                        resDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section 3: Native Accessibility Service Status & Simulator Mode
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                    .testTag("accessibility_settings_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ACCESSIBILITY SERVICE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    if (isServiceActive) SuccessGreen.copy(alpha = 0.2f) else Color(0xFFB45309).copy(alpha = 0.2f),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isServiceActive) SuccessGreen else Color(0xFFF59E0B),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isServiceActive) "CONNECTED" else "NOT ENABLED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isServiceActive) SuccessGreen else Color(0xFFF59E0B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "MacroExecutorService uses dispatchGesture(GestureDescription) for tap/swipe and takeScreenshot() for screen capture. To enable on real devices, grant permission in Android Accessibility Settings.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open settings: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanGlow)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Android Accessibility Settings")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulation Mode Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "High-Fidelity Simulation Mode",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Allows full testing in emulator/preview without accessibility permissions.",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Switch(
                            checked = isSimulationMode,
                            onCheckedChange = { settingsManager.setSimulationMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ElectricBlue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Speed Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Step Delay Speed", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Text("${currentDelayMs}ms", fontSize = 12.sp, color = CyanGlow, fontWeight = FontWeight.Bold)
                    }

                    Slider(
                        value = currentDelayMs.toFloat(),
                        onValueChange = { settingsManager.setStepDelayMs(it.toLong()) },
                        valueRange = 500f..4000f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanGlow,
                            activeTrackColor = ElectricBlue,
                            inactiveTrackColor = Color(0xFF1E293B)
                        )
                    )
                }
            }
        }
    }
}
