package com.kallistocore.ai.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kallistocore.ai.ui.theme.LocalKallistoColors

@Composable
fun CompactFloatingOverlay(
    onExpand: () -> Unit
) {
    val colors = LocalKallistoColors.current
    var isMicListening by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(270.dp)
            .wrapContentHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .border(1.5.dp, colors.border, RoundedCornerShape(24.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Bar: Drag handle indicator & Expand button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.statusSuccess)
                    )
                    Text(
                        text = "Kallisto Core",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                IconButton(
                    onClick = onExpand,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInFull,
                        contentDescription = "Expand to Full Screen",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Compact Audio Pulse Waveform Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceVariant)
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isMicListening) "Listening locally..." else "Kokoro Voice & Memory Active",
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Medium
                    )

                    // Waveform visualizer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(14) { index ->
                            val height = if (isMicListening) (6 + ((index * 5) % 16)).dp else 5.dp
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(height)
                                    .clip(CircleShape)
                                    .background(if (isMicListening) colors.accentWave else colors.primary)
                            )
                        }
                    }
                }
            }

            // Micro Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Toggle
                IconButton(
                    onClick = { isMicListening = !isMicListening },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isMicListening) colors.primary else colors.surfaceVariant)
                ) {
                    Icon(
                        imageVector = if (isMicListening) Icons.Rounded.Mic else Icons.Rounded.MicNone,
                        contentDescription = "Microphone",
                        tint = if (isMicListening) colors.onPrimary else colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Quick Full Chat launcher pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.surfaceVariant)
                        .clickable { onExpand() }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Chat,
                        contentDescription = "Chat",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Open Chat",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
}
