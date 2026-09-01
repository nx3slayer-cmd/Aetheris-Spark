package com.kallistocore.ai.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kallistocore.ai.domain.tts.TtsStatus
import com.kallistocore.ai.ui.theme.LocalKallistoColors
import com.kallistocore.ai.ui.viewmodel.CompanionViewModel
import com.kallistocore.ai.ui.viewmodel.MainTab
import com.kallistocore.ai.ui.viewmodel.UiChatMessage
import java.io.File

@Composable
fun ChatScreen(viewModel: CompanionViewModel) {
    val colors = LocalKallistoColors.current
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val messages by viewModel.chatMessages.collectAsState()
    val ttsState by viewModel.ttsPlaybackState.collectAsState()
    val isAutoVoice by viewModel.isVoiceAutoSpeak.collectAsState()
    val llmState by viewModel.llmEngine.engineState.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
            selectedImageBitmap = bitmap
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Active Model Status Banner & Quick Model Hub Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                .clickable { viewModel.selectTab(MainTab.SETTINGS) }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.statusSuccess)
                )
                Text(
                    text = "Active Engine: ${llmState.loadedModelName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            }
            Text(
                text = "Model Hub →",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.accentWave
            )
        }

        // Message Stream Area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 14.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    EmptyChatGreeting(onOpenModelHub = { viewModel.selectTab(MainTab.SETTINGS) })
                }
            }

            items(messages, key = { it.id }) { msg ->
                ChatBubbleItem(
                    msg = msg,
                    isSpeaking = ttsState.status == TtsStatus.PLAYING,
                    amplitude = ttsState.currentAmplitude,
                    onPlayAudio = { viewModel.testKokoroVoice(msg.text) }
                )
            }
        }

        // Thumb Input Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Secondary Action Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surfaceVariant)
                                .clickable {
                                    messages.lastOrNull { it.isUser }?.let { lastUserMsg ->
                                        viewModel.sendMessage(lastUserMsg.text)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "Regenerate", tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                            Text(text = "Regenerate", fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surfaceVariant)
                                .clickable { viewModel.clearChatHistory() }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.DeleteOutline, contentDescription = "Clear", tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                            Text(text = "Clear", fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
                        }
                    }

                    Text(
                        text = if (isAutoVoice) "Kokoro Audio: Auto-Play" else "Kokoro Audio: Muted",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isAutoVoice) colors.accentWave else colors.textSecondary
                    )
                }

                // Selected Image Thumbnail Preview
                if (selectedImageBitmap != null) {
                    Row(
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surfaceVariant)
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Image, contentDescription = "Attached", tint = colors.primary, modifier = Modifier.size(20.dp))
                        Text(text = "Photo loaded for Img2Img transformation", fontSize = 12.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
                        IconButton(onClick = { selectedImageBitmap = null }, modifier = Modifier.size(20.dp)) {
                            Icon(imageVector = Icons.Rounded.Close, contentDescription = "Remove", tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Main Input Field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(28.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (selectedImageBitmap != null) colors.primary.copy(alpha = 0.15f) else Color.Transparent)
                    ) {
                        Icon(imageVector = Icons.Rounded.AddPhotoAlternate, contentDescription = "Attach", tint = if (selectedImageBitmap != null) colors.primary else colors.textSecondary, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = { viewModel.isVoiceAutoSpeak.value = !isAutoVoice },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isAutoVoice) colors.accentWave.copy(alpha = 0.15f) else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = if (isAutoVoice) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                            contentDescription = "Audio Toggle",
                            tint = if (isAutoVoice) colors.accentWave else colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        textStyle = TextStyle(color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Normal),
                        cursorBrush = SolidColor(colors.primary),
                        decorationBox = { innerTextField ->
                            if (inputText.isEmpty() && selectedImageBitmap == null) {
                                Text(text = "Chat, search, or edit photos...", color = colors.textSecondary, fontSize = 14.sp)
                            }
                            innerTextField()
                        }
                    )

                    val canSend = inputText.isNotBlank() || selectedImageBitmap != null
                    IconButton(
                        onClick = {
                            if (canSend) {
                                viewModel.sendMessage(inputText.trim(), selectedImageBitmap)
                                inputText = ""
                                selectedImageBitmap = null
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (canSend) colors.primary else colors.surfaceVariant)
                    ) {
                        Icon(imageVector = Icons.Rounded.ArrowUpward, contentDescription = "Send", tint = if (canSend) colors.onPrimary else colors.textSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    msg: UiChatMessage,
    isSpeaking: Boolean,
    amplitude: Float,
    onPlayAudio: () -> Unit
) {
    val colors = LocalKallistoColors.current
    val alignment = if (msg.isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (msg.isUser) colors.userBubble else colors.aiBubble
    val textColor = if (msg.isUser) colors.onPrimary else colors.textPrimary

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (msg.isUser) 20.dp else 4.dp,
                        bottomEnd = if (msg.isUser) 4.dp else 20.dp
                    )
                )
                .background(bubbleColor)
                .border(
                    width = if (msg.isUser) 0.dp else 1.dp,
                    color = colors.border,
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (msg.isUser) 20.dp else 4.dp,
                        bottomEnd = if (msg.isUser) 4.dp else 20.dp
                    )
                )
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (msg.imagePath != null) {
                    AsyncImage(
                        model = File(msg.imagePath),
                        contentDescription = "Image Attachment",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                if (msg.text.isNotBlank()) {
                    Text(text = msg.text, color = textColor, fontSize = 14.5.sp, lineHeight = 21.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!msg.isUser) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onPlayAudio() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.VolumeUp, contentDescription = "Play Voice", tint = colors.accentWave, modifier = Modifier.size(15.dp))
                            Text(text = "Speak", fontSize = 11.sp, color = colors.accentWave, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Text(text = msg.timestamp, fontSize = 10.sp, color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
fun EmptyChatGreeting(onOpenModelHub: () -> Unit) {
    val colors = LocalKallistoColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surfaceVariant)
                .border(1.dp, colors.border, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Rounded.AutoAwesome, contentDescription = "Kallisto", tint = colors.primary, modifier = Modifier.size(28.dp))
        }
        Text(text = "Kallisto Core Ready", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        Text(text = "Local LLM • Kokoro-82M Voice • On-Device Img2Img", fontSize = 12.sp, color = colors.textSecondary)

        Button(
            onClick = onOpenModelHub,
            modifier = Modifier
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(12.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant)
        ) {
            Text("Browse & Download Models in Model Hub", fontSize = 12.sp, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}
