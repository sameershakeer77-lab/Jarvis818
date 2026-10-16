package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ActionBlock
import com.example.model.ActionBlockType
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.MacroActionBlue

@Composable
fun EditBlockDialog(
    block: ActionBlock,
    onDismiss: () -> Unit,
    onSave: (ActionBlock) -> Unit
) {
    var title by remember { mutableStateOf(block.title) }
    var description by remember { mutableStateOf(block.description) }

    var coordX by remember { mutableStateOf(block.targetX?.toString() ?: "940") }
    var coordY by remember { mutableStateOf(block.targetY?.toString() ?: "140") }
    var label by remember { mutableStateOf(block.label) }

    var textInput by remember { mutableStateOf(block.textToInput) }
    var waitSeconds by remember { mutableStateOf(block.waitSeconds.toString()) }
    var appPackage by remember { mutableStateOf(block.packageName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Edit Action Block #${block.stepNumber}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Type: ${block.type.displayName}",
                    fontSize = 12.sp,
                    color = CyanGlow
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                when (block.type) {
                    ActionBlockType.TAP_COORDINATES -> {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = coordX,
                                onValueChange = { coordX = it },
                                label = { Text("X (0..1080)") },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF1E293B),
                                    unfocusedContainerColor = Color(0xFF1E293B),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = coordY,
                                onValueChange = { coordY = it },
                                label = { Text("Y (0..2400)") },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF1E293B),
                                    unfocusedContainerColor = Color(0xFF1E293B),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            label = { Text("Element Label") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                    ActionBlockType.INPUT_TEXT -> {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            label = { Text("Text to Input") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                    ActionBlockType.WAIT_BEFORE_NEXT -> {
                        OutlinedTextField(
                            value = waitSeconds,
                            onValueChange = { waitSeconds = it },
                            label = { Text("Seconds to wait") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                    ActionBlockType.LAUNCH_APP -> {
                        OutlinedTextField(
                            value = appPackage,
                            onValueChange = { appPackage = it },
                            label = { Text("Package Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newParams = block.params.toMutableMap()
                            when (block.type) {
                                ActionBlockType.TAP_COORDINATES -> {
                                    newParams["x"] = coordX.trim()
                                    newParams["y"] = coordY.trim()
                                    newParams["label"] = label.trim()
                                }
                                ActionBlockType.INPUT_TEXT -> {
                                    newParams["text"] = textInput
                                }
                                ActionBlockType.WAIT_BEFORE_NEXT -> {
                                    newParams["seconds"] = waitSeconds.trim()
                                }
                                ActionBlockType.LAUNCH_APP -> {
                                    newParams["packageName"] = appPackage.trim()
                                }
                                else -> {}
                            }
                            onSave(
                                block.copy(
                                    title = title,
                                    description = description,
                                    params = newParams
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MacroActionBlue)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}
