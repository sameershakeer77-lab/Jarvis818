package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MacroRepository
import com.example.engine.MacroEngine
import com.example.model.ActionBlock
import com.example.model.Macro
import com.example.ui.components.ActionBlockCard
import com.example.ui.components.EditBlockDialog
import com.example.ui.components.FlowConnectorArrow
import com.example.ui.components.TriggerBlockCard
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MacroActionBlue
import com.example.ui.theme.MacroTriggerRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavedMacrosScreen(
    repository: MacroRepository,
    engine: MacroEngine,
    onNavigateToLive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val macros by repository.savedMacros.collectAsState()
    var expandedMacroId by remember { mutableStateOf<String?>(null) }
    var editingBlockInfo by remember { mutableStateOf<Pair<String, ActionBlock>?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 90.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "SAVED MACROS",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "AI-constructed flows ready for 1-tap replay & coordinate fine-tuning",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${macros.size} Saved",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanGlow
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (macros.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    color = Color(0xFF111827)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Macros Saved Yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Go to the Live Constructor tab and enter a prompt to build your first AI macro!",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToLive,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                        ) {
                            Text("Open Live Constructor")
                        }
                    }
                }
            }
        }

        items(macros, key = { it.id }) { macro ->
            val isExpanded = expandedMacroId == macro.id
            SavedMacroCard(
                macro = macro,
                isExpanded = isExpanded,
                onToggleExpand = {
                    expandedMacroId = if (isExpanded) null else macro.id
                },
                onReplay = {
                    engine.replayMacro(macro)
                    onNavigateToLive()
                },
                onDuplicate = { repository.duplicateMacro(macro) },
                onDelete = { repository.deleteMacro(macro.id) },
                onEditBlock = { block ->
                    editingBlockInfo = macro.id to block
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // Edit Coordinates / Params Dialog
    editingBlockInfo?.let { (macroId, block) ->
        EditBlockDialog(
            block = block,
            onDismiss = { editingBlockInfo = null },
            onSave = { updatedBlock ->
                repository.updateActionBlock(macroId, updatedBlock)
                editingBlockInfo = null
            }
        )
    }
}

@Composable
fun SavedMacroCard(
    macro: Macro,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onReplay: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onEditBlock: (ActionBlock) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }
    val formattedDate = remember(macro.createdAt) { dateFormat.format(Date(macro.createdAt)) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, if (isExpanded) CyanGlow else Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .testTag("saved_macro_${macro.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Title & Replay Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = macro.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Button(
                    onClick = onReplay,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("replay_button_${macro.id}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Replay", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges row: Trigger pill + Action Block Count pill + Target Res
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(MacroTriggerRed.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .border(1.dp, MacroTriggerRed.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "1 Trigger",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MacroTriggerRed
                    )
                }

                Box(
                    modifier = Modifier
                        .background(MacroActionBlue.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .border(1.dp, MacroActionBlue.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${macro.actions.size} Actions",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanGlow
                    )
                }

                Text(
                    text = "• Runs: ${macro.executionCount}",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action footer: Toggle Flow Builder view, Duplicate, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleExpand() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "Hide MacroDroid Flow" else "View / Edit Action Blocks",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanGlow
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = CyanGlow,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row {
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Expanded Flow Builder Section
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = "FLOW BUILDER ARCHITECTURE (TAP ACTION TO EDIT COORDINATES):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Trigger block (Red)
                    TriggerBlockCard(
                        triggerText = macro.trigger,
                        prompt = macro.sourcePrompt
                    )

                    // Stack of action blocks (Blue)
                    macro.actions.forEach { block ->
                        FlowConnectorArrow()
                        ActionBlockCard(
                            block = block,
                            onEditClick = { onEditBlock(block) }
                        )
                    }
                }
            }
        }
    }
}
