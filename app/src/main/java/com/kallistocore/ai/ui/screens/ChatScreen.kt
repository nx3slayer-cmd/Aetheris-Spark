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
import com.kallistocore.ai.ui.viewmodel.UiChatMessage
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ChatScreen(viewModel: CompanionViewModel) {
    val colors = LocalKallistoColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages by viewModel.chatMessages.collectAsState()
    val ttsState by viewModel.ttsPlaybackState.collectAsState()
    val isAutoVoice by viewModel.isVoiceAutoSpeak.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Image Picker for Img2Img editing & visual input
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
        // Message Stream Area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 14.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    EmptyChatGreeting()
                }
            }

            items(messages, key = { it.id }) { msg ->
                ChatBubbleItem(
                    msg = msg,
                    isSpeaking = ttsState.status == TtsStatus.PLAYING && msg.audioPath != null,
                    amplitude = ttsState.currentAmplitude
                )
            }
        }

        // Thumb-Accessible Bottom Input Control Area
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Secondary Pills: Quick actions & Attached Image preview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Regenerate Pill
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
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Regenerate",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Regenerate",
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Clear Chat Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surfaceVariant)
                                .clickable { viewModel.clearChatHistory() }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Clear",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Clear",
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Voice Output Mode Indicator
                    Text(
                        text = if (isAutoVoice) "Kokoro Voice: On" else "Kokoro Voice: Muted",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isAutoVoice) colors.accentWave else colors.textSecondary
                    )
                }

                // Selected Image Thumbnail Preview for Img2Img
                if (selectedImageBitmap != null) {
                    Row(
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surfaceVariant)
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = "Image Attached",
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Photo loaded for Img2Img editing",
                            fontSize = 12.sp,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(
                            onClick = { selectedImageBitmap = null },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Remove",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Main Bento Input Pill Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(28.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Image / Img2Img Picker Button
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (selectedImageBitmap != null) colors.primary.copy(alpha = 0.15f) else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = "Attach Image",
                            tint = if (selectedImageBitmap != null) colors.primary else colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Kokoro Voice Toggle Button
                    IconButton(
                        onClick = { viewModel.isVoiceAutoSpeak.value = !isAutoVoice },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isAutoVoice) colors.accentWave.copy(alpha = 0.15f) else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = if (isAutoVoice) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                            contentDescription = "Toggle Kokoro Audio",
                            tint = if (isAutoVoice) colors.accentWave else colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Message Text Input Field
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        textStyle = TextStyle(
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(colors.primary),
                        decorationBox = { innerTextField ->
                            if (inputText.isEmpty() && selectedImageBitmap == null) {
                                Text(
                                    text = "Ask, search, or edit photos...",
                                    color = colors.textSecondary,
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    // Send Button
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
                        Icon(
                            imageVector = Icons.Rounded.ArrowUpward,
                            contentDescription = "Send",
                            tint = if (canSend) colors.onPrimary else colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
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
    amplitude: Float
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
                // Attached or Generated Image Display
                if (msg.imagePath != null) {
                    AsyncImage(
                        model = File(msg.imagePath),
                        contentDescription = "Artwork Attachment",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                // Message Text Content
                if (msg.text.isNotBlank()) {
                    Text(
                        text = msg.text,
                        color = textColor,
                        fontSize = 14.5.sp,
                        lineHeight = 21.sp
                    )
                }

                // Kokoro Audio Waveform / Metadata Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!msg.isUser && msg.audioPath != null) {
                        // Live or static waveform bars
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(6) { index ->
                                val height = if (isSpeaking) {
                                    (6 + (amplitude * 20 * (index + 1) % 18)).dp
                                } else {
                                    (6 + (index * 2)).dp
                                }
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(height)
                                        .clip(CircleShape)
                                        .background(colors.accentWave)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Kokoro Voice",
                                fontSize = 10.sp,
                                color = colors.accentWave,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Text(
                        text = msg.timestamp,
                        fontSize = 10.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyChatGreeting() {
    val colors = LocalKallistoColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surfaceVariant)
                .border(1.dp, colors.border, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = "AI",
                tint = colors.primary,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = "Kallisto Core Ready",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
        Text(
            text = "Local LLM • Kokoro-82M TTS • On-Device Search & Vision",
            fontSize = 12.sp,
            color = colors.textSecondary
        )
    }
}
