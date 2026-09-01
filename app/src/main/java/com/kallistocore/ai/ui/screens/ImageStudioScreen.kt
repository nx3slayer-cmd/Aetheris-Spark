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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kallistocore.ai.domain.image.ImageGenState
import com.kallistocore.ai.ui.theme.LocalKallistoColors
import com.kallistocore.ai.ui.viewmodel.CompanionViewModel
import kotlinx.coroutines.launch

@Composable
fun ImageStudioScreen(viewModel: CompanionViewModel) {
    val colors = LocalKallistoColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val progressState by viewModel.imageProgressState.collectAsState()
    val sourceBitmap by viewModel.selectedSourceImage.collectAsState()
    val strength by viewModel.img2imgStrength.collectAsState()

    var promptText by remember { mutableStateOf("") }
    var diffusionSteps by remember { mutableFloatStateOf(4f) }

    val isGenerating = progressState.state == ImageGenState.PREPROCESSING ||
            progressState.state == ImageGenState.DENOISING_STEPS ||
            progressState.state == ImageGenState.POSTPROCESSING

    // Photo Picker Launcher for Img2Img
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bmp = if (Build.VERSION.SDK_INT < 28) {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
            viewModel.selectedSourceImage.value = bmp
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // Studio Header Bento
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
                        imageVector = Icons.Rounded.Brush,
                        contentDescription = "Studio",
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Image Studio & Img2Img Editor",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
                Text(
                    text = "Generate visuals from text or transform existing photos with on-device diffusion pipelines.",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    lineHeight = 17.sp
                )
            }
        }

        // Source Photo Picker Bento (For Img2Img)
        item {
            Text(
                text = "Source Image (Optional for Img2Img)",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    .clickable { photoPickerLauncher.launch("image/*") }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                if (sourceBitmap != null) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Image,
                                    contentDescription = "Selected Photo",
                                    tint = colors.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Photo Loaded for Editing",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Tap to choose a different image",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.selectedSourceImage.value = null }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Cancel,
                                contentDescription = "Clear",
                                tint = colors.error
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = "Import",
                            tint = colors.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Tap to load a photo for Img2Img editing",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Leave empty to generate from text prompt only",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }

        // Prompt Input Bento
        item {
            Text(
                text = "Prompt & Transformation Instructions",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                BasicTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = TextStyle(
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        lineHeight = 19.sp
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    decorationBox = { innerTextField ->
                        if (promptText.isEmpty()) {
                            Text(
                                text = if (sourceBitmap != null) "E.g., Make it look like an oil painting in autumn..." else "E.g., A cozy cyberpunk library at night, 8k...",
                                color = colors.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        // Controls Bento (Strength & Steps)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (sourceBitmap != null) {
                    // Img2Img Transformation Strength
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Transformation Strength", fontSize = 13.sp, color = colors.textPrimary)
                            Text("${(strength * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                        }
                        Slider(
                            value = strength,
                            onValueChange = { viewModel.img2imgStrength.value = it },
                            valueRange = 0.1f..1.0f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.primary,
                                activeTrackColor = colors.primary,
                                inactiveTrackColor = colors.surfaceVariant
                            )
                        )
                    }
                }

                // Diffusion Steps
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Denoising Steps", fontSize = 13.sp, color = colors.textPrimary)
                        Text("${diffusionSteps.toInt()} Steps", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                    }
                    Slider(
                        value = diffusionSteps,
                        onValueChange = { diffusionSteps = it },
                        valueRange = 2f..8f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.primary,
                            activeTrackColor = colors.primary,
                            inactiveTrackColor = colors.surfaceVariant
                        )
                    )
                }
            }
        }

        // Action CTA & Progress Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isGenerating) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Synthesizing: Step ${progressState.currentStep} of ${progressState.totalSteps}",
                                fontSize = 12.sp,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(progressState.progressFraction * 100).toInt()}%",
                                fontSize = 12.sp,
                                color = colors.accentWave,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progressState.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = colors.primary,
                            trackColor = colors.surfaceVariant,
                        )
                    }
                }

                Button(
                    onClick = {
                        if (!isGenerating && promptText.isNotBlank()) {
                            coroutineScope.launch {
                                viewModel.imageStudio.generateOrEditImage(
                                    prompt = promptText,
                                    inputImage = sourceBitmap,
                                    strength = strength,
                                    steps = diffusionSteps.toInt()
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    enabled = !isGenerating && promptText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Icon(
                        imageVector = if (sourceBitmap != null) Icons.Rounded.AutoFixHigh else Icons.Rounded.Palette,
                        contentDescription = "Execute",
                        tint = colors.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isGenerating) "Processing On-Device Diffusion..." else if (sourceBitmap != null) "Transform Image (Img2Img)" else "Generate Image",
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onPrimary
                    )
                }
            }
        }

        // Output Result Card
        if (progressState.generatedFile != null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Generated Artwork Result",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )

                    AsyncImage(
                        model = progressState.generatedFile,
                        contentDescription = "Output Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.sendMessage("Generated on-device artwork: $promptText")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant)
                        ) {
                            Text("Send to Chat", color = colors.textPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
