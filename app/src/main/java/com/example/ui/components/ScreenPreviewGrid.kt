package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.TargetCrosshair
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.TargetRed
import com.example.ui.theme.TargetRipple

@Composable
fun ScreenPreviewGrid(
    bitmap: Bitmap?,
    stageName: String,
    crosshair: TargetCrosshair?,
    modifier: Modifier = Modifier,
    targetResolution: String = "1080x2400 (Vivo Y300 5G)"
) {
    var showGrid by remember { mutableStateOf(true) }
    var inspectedCoord by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "target_ripple")
    val rippleRadius by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 36f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleRadius"
    )
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleAlpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .testTag("screen_preview_card"),
        color = Color(0xFF0B1019),
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = null,
                    tint = CyanGlow,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LIVE SCREEN: $stageName",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = targetResolution,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Grid Toggle Button
                IconButton(
                    onClick = { showGrid = !showGrid },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = "Toggle Grid",
                        tint = if (showGrid) CyanGlow else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Interactive Scaled Device Screen (Aspect ratio 9:19.5 or 1080:2400)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .aspectRatio(1080f / 2400f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF030712))
                    .border(2.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val screenX = (offset.x / size.width * 1080).toInt().coerceIn(0, 1080)
                            val screenY = (offset.y / size.height * 2400).toInt().coerceIn(0, 2400)
                            inspectedCoord = screenX to screenY
                        }
                    }
            ) {
                val containerWidth = maxWidth
                val containerHeight = maxHeight

                // Render current device frame bitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Live device screen",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Render Coordinate Grid Overlay & Red Target Dot
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasW = size.width
                    val canvasH = size.height

                    if (showGrid) {
                        // Draw vertical coordinate lines every 200px (1080 scale)
                        for (x in 200..1000 step 200) {
                            val px = (x / 1080f) * canvasW
                            drawLine(
                                color = Color(0x3338BDF8),
                                start = Offset(px, 0f),
                                end = Offset(px, canvasH),
                                strokeWidth = 1f
                            )
                        }

                        // Draw horizontal coordinate lines every 300px (2400 scale)
                        for (y in 300..2100 step 300) {
                            val py = (y / 2400f) * canvasH
                            drawLine(
                                color = Color(0x3338BDF8),
                                start = Offset(0f, py),
                                end = Offset(canvasW, py),
                                strokeWidth = 1f
                            )
                        }
                    }

                    // Draw AI Target Red Dot with Crosshairs and Pulsing Ripple
                    crosshair?.let { target ->
                        val targetPxX = (target.x / 1080f) * canvasW
                        val targetPxY = (target.y / 2400f) * canvasH

                        // Pulsing outer ripple ring
                        drawCircle(
                            color = TargetRed.copy(alpha = rippleAlpha),
                            radius = rippleRadius,
                            center = Offset(targetPxX, targetPxY),
                            style = Stroke(width = 2.5f)
                        )

                        // Crosshair lines
                        drawLine(
                            color = TargetRed,
                            start = Offset(targetPxX - 18f, targetPxY),
                            end = Offset(targetPxX + 18f, targetPxY),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = TargetRed,
                            start = Offset(targetPxX, targetPxY - 18f),
                            end = Offset(targetPxX, targetPxY + 18f),
                            strokeWidth = 2f
                        )

                        // Inner solid red target dot
                        drawCircle(
                            color = TargetRed,
                            radius = 6f,
                            center = Offset(targetPxX, targetPxY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5f,
                            center = Offset(targetPxX, targetPxY)
                        )
                    }

                    // Draw User Touch-Inspected point if active
                    inspectedCoord?.let { (ix, iy) ->
                        val ipx = (ix / 1080f) * canvasW
                        val ipy = (iy / 2400f) * canvasH
                        drawCircle(
                            color = CyanGlow,
                            radius = 8f,
                            center = Offset(ipx, ipy),
                            style = Stroke(width = 2f)
                        )
                    }
                }

                // AI Target Info Badge floating at bottom of preview
                if (crosshair != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp)
                            .background(Color(0xE60F172A), RoundedCornerShape(8.dp))
                            .border(1.dp, TargetRed, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(TargetRed, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI Target: (${crosshair.x}, ${crosshair.y}) • ${crosshair.label}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else if (inspectedCoord != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp)
                            .background(Color(0xE60F172A), RoundedCornerShape(8.dp))
                            .border(1.dp, CyanGlow, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Inspected: (X: ${inspectedCoord?.first}, Y: ${inspectedCoord?.second})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanGlow
                        )
                    }
                }
            }

            // Footer hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Tap preview anytime to inspect pixel coordinates • Grid: 200x300px",
                    fontSize = 10.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
