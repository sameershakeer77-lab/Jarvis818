package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActionBlock
import com.example.model.ActionBlockType
import com.example.model.BlockStatus
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.MacroActionBlue
import com.example.ui.theme.MacroActionBlueDark
import com.example.ui.theme.MacroTriggerRed
import com.example.ui.theme.MacroTriggerRedDark
import com.example.ui.theme.SuccessGreen

/**
 * Visual MacroDroid Trigger Block (Iconic Red Card)
 */
@Composable
fun TriggerBlockCard(
    triggerText: String,
    prompt: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, MacroTriggerRed, RoundedCornerShape(14.dp))
            .testTag("trigger_block_card"),
        color = Color(0xFF200F14),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MacroTriggerRed, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Trigger Icon",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "TRIGGER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MacroTriggerRed,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = triggerText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(MacroTriggerRedDark.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (prompt.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"$prompt\"",
                    fontSize = 13.sp,
                    color = Color(0xFFFDA4AF),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Connecting Flow Arrow between MacroDroid blocks
 */
@Composable
fun FlowConnectorArrow(
    modifier: Modifier = Modifier,
    active: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(14.dp)
                .background(if (active) CyanGlow else Color(0xFF334155))
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Flow direction arrow",
            tint = if (active) CyanGlow else Color(0xFF64748B),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Visual MacroDroid Action Block (Iconic Blue Card)
 */
@Composable
fun ActionBlockCard(
    block: ActionBlock,
    modifier: Modifier = Modifier,
    onEditClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val isExecuting = block.status == BlockStatus.EXECUTING
    val cardModifier = if (isExecuting) {
        modifier.scale(pulseScale)
    } else {
        modifier
    }

    val borderColor = when (block.status) {
        BlockStatus.EXECUTING -> CyanGlow
        BlockStatus.COMPLETED -> SuccessGreen.copy(alpha = 0.7f)
        BlockStatus.FAILED -> MaterialTheme.colorScheme.error
        else -> MacroActionBlue.copy(alpha = 0.5f)
    }

    Surface(
        modifier = cardModifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(if (isExecuting) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(enabled = onEditClick != null) { onEditClick?.invoke() }
            .testTag("action_block_${block.stepNumber}"),
        color = if (isExecuting) Color(0xFF101C38) else Color(0xFF0F172A),
        shadowElevation = if (isExecuting) 8.dp else 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header Row: Step Number + Icon + Title + Status Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Action Block Icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isExecuting) CyanGlow else MacroActionBlue,
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getActionIcon(block.type),
                            contentDescription = block.type.displayName,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ACTION #${block.stepNumber}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isExecuting) CyanGlow else MacroActionBlue,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${block.type.category}",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Text(
                            text = block.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // Status Indicator
                when (block.status) {
                    BlockStatus.EXECUTING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = CyanGlow,
                            strokeWidth = 2.5.dp
                        )
                    }
                    BlockStatus.COMPLETED -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = SuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            if (block.executionTimeMs != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${block.executionTimeMs}ms",
                                    fontSize = 11.sp,
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    BlockStatus.PENDING -> {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = "Pending",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    BlockStatus.FAILED -> {
                        Text(
                            text = "FAILED",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    BlockStatus.SKIPPED -> {
                        Text(
                            text = "SKIPPED",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Description
            Text(
                text = block.description,
                fontSize = 12.sp,
                color = Color(0xFFCBD5E1),
                lineHeight = 16.sp
            )

            // Specific Parameters Pills (Coordinates, text, app name)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BlockParameterChips(block)

                if (onEditClick != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Edit",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockParameterChips(block: ActionBlock) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        when (block.type) {
            ActionBlockType.TAP_COORDINATES -> {
                block.targetX?.let { x ->
                    block.targetY?.let { y ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1E3A8A), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "X: $x  Y: $y",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanGlow
                            )
                        }
                    }
                }
            }
            ActionBlockType.INPUT_TEXT -> {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF0F2E3D), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "\"${block.textToInput.take(20)}\"",
                        fontSize = 11.sp,
                        color = Color(0xFF7DD3FC)
                    )
                }
            }
            ActionBlockType.WAIT_BEFORE_NEXT -> {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF332005), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${block.waitSeconds} seconds",
                        fontSize = 11.sp,
                        color = Color(0xFFFBBF24)
                    )
                }
            }
            ActionBlockType.LAUNCH_APP -> {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = block.appName.ifBlank { block.packageName.take(18) },
                        fontSize = 11.sp,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
            ActionBlockType.TAKE_SCREENSHOT_AND_ANALYZE -> {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF2A153A), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "1080x2400 Vivo Y300 5G",
                        fontSize = 11.sp,
                        color = Color(0xFFE879F9)
                    )
                }
            }
            ActionBlockType.SWIPE_COORDINATES -> {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF1E3A8A), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "(${block.swipeX1},${block.swipeY1}) → (${block.swipeX2},${block.swipeY2})",
                        fontSize = 11.sp,
                        color = CyanGlow
                    )
                }
            }
        }
    }
}

fun getActionIcon(type: ActionBlockType): ImageVector {
    return when (type) {
        ActionBlockType.LAUNCH_APP -> Icons.Default.Launch
        ActionBlockType.WAIT_BEFORE_NEXT -> Icons.Default.Timer
        ActionBlockType.TAKE_SCREENSHOT_AND_ANALYZE -> Icons.Default.PhotoCamera
        ActionBlockType.TAP_COORDINATES -> Icons.Default.TouchApp
        ActionBlockType.INPUT_TEXT -> Icons.Default.Keyboard
        ActionBlockType.SWIPE_COORDINATES -> Icons.Default.Navigation
    }
}
