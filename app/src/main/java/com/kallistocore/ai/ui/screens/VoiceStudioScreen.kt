package com.kallistocore.ai.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.kallistocore.ai.domain.tts.TtsStatus
import com.kallistocore.ai.ui.theme.LocalKallistoColors
import com.kallistocore.ai.ui.viewmodel.CompanionViewModel

@Composable
fun VoiceStudioScreen(viewModel: CompanionViewModel) {
    val colors = LocalKallistoColors.current
    val ttsState by viewModel.ttsPlaybackState.collectAsState()
    val selectedVoice by viewModel.selectedVoiceProfile.collectAsState()
    val speed by viewModel.speechSpeed.collectAsState()
    val pitch by viewModel.speechPitch.collectAsState()

    val isAudioPlaying = ttsState.status == TtsStatus.PLAYING || ttsState.status == TtsStatus.SYNTHESIZING

    val voices = listOf(
        "af_heart (Warm American)",
        "af_bella (Expressive Female)",
        "af_nicole (Soft Narrative)",
        "am_adam (Deep Resonance Male)",
        "am_michael (Crisp Clear Male)",
        "bf_emma (British Natural)"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // Engine Header Bento Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = "TTS Engine",
                        tint = colors.accentWave,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Kokoro-82M Voice Studio",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
                Text(
                    text = "High-fidelity, 82M parameter neural speech synthesizer running 100% on-device at 24,000 Hz.",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    lineHeight = 17.sp
                )
            }
        }

        // Voice Profile Selection Grid
        item {
            Text(
                text = "Voice Personas",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                voices.forEach { voice ->
                    val isSelected = voice == selectedVoice
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) colors.surfaceVariant else colors.surface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) colors.primary else colors.border,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.selectedVoiceProfile.value = voice }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) colors.primary else colors.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.RecordVoiceOver,
                                    contentDescription = "Voice",
                                    tint = if (isSelected) colors.onPrimary else colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = voice,
                                fontSize = 13.5.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = colors.textPrimary
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Active",
                                tint = colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Speed & Pitch Sliders Bento Box
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Speed Controller
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Speech Rate", fontSize = 13.sp, color = colors.textPrimary)
                        Text(
                            text = "${String.format("%.1f", speed)}x",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accentWave
                        )
                    }
                    Slider(
                        value = speed,
                        onValueChange = { viewModel.speechSpeed.value = it },
                        valueRange = 0.5f..2.0f,
                        steps = 15,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.primary,
                            activeTrackColor = colors.primary,
                            inactiveTrackColor = colors.surfaceVariant
                        )
                    )
                }

                // Pitch Controller
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pitch Scale", fontSize = 13.sp, color = colors.textPrimary)
                        Text(
                            text = "${String.format("%.1f", pitch)}x",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accentWave
                        )
                    }
                    Slider(
                        value = pitch,
                        onValueChange = { viewModel.speechPitch.value = it },
                        valueRange = 0.7f..1.3f,
                        steps = 6,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.primary,
                            activeTrackColor = colors.primary,
                            inactiveTrackColor = colors.surfaceVariant
                        )
                    )
                }
            }
        }

        // Real-Time Audio Tester & Animated Waveform
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Live Waveform Visualizer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceVariant)
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(24) { index ->
                        val barHeight = if (isAudioPlaying) {
                            (6 + (ttsState.currentAmplitude * 26 * ((index % 5) + 1))).dp
                        } else {
                            4.dp
                        }
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(barHeight)
                                .clip(CircleShape)
                                .background(if (isAudioPlaying) colors.accentWave else colors.textSecondary.copy(alpha = 0.4f))
                        )
                    }
                }

                // Test Button
                Button(
                    onClick = {
                        if (isAudioPlaying) {
                            viewModel.ttsEngine.stopAudio()
                        } else {
                            viewModel.testKokoroVoice()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Icon(
                        imageVector = if (isAudioPlaying) Icons.Rounded.Stop else Icons.Rounded.VolumeUp,
                        contentDescription = "Test Audio",
                        tint = colors.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAudioPlaying) "Stop Voice Playback" else "Test Kokoro Voice Sample",
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onPrimary
                    )
                }
            }
        }
    }
}
