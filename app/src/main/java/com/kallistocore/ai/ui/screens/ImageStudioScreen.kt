package com.kallistocore.ai.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kallistocore.ai.domain.image.AspectRatioOption
import com.kallistocore.ai.domain.image.ImageGenState
import com.kallistocore.ai.ui.theme.LocalKallistoColors
import com.kallistocore.ai.ui.viewmodel.CompanionViewModel
import kotlinx.coroutines.launch

enum class StudioMode {
    QUICK_STUDIO,
    NODE_WORKFLOW_COMFYUI
}

@Composable
fun ImageStudioScreen(viewModel: CompanionViewModel) {
    val colors = LocalKallistoColors.current
    var studioMode by remember { mutableStateOf(StudioMode.QUICK_STUDIO) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top Studio Mode Toggle (Quick Studio vs Node Canvas ComfyUI)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (studioMode == StudioMode.QUICK_STUDIO) colors.primary else Color.Transparent)
                    .clickable { studioMode = StudioMode.QUICK_STUDIO }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Quick Studio",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (studioMode == StudioMode.QUICK_STUDIO) colors.onPrimary else colors.textSecondary
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (studioMode == StudioMode.NODE_WORKFLOW_COMFYUI) colors.primary else Color.Transparent)
                    .clickable { studioMode = StudioMode.NODE_WORKFLOW_COMFYUI }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.AccountTree, contentDescription = "Nodes", tint = if (studioMode == StudioMode.NODE_WORKFLOW_COMFYUI) colors.onPrimary else colors.accentWave, modifier = Modifier.size(14.dp))
                    Text(
                        text = "ComfyUI Node Canvas",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (studioMode == StudioMode.NODE_WORKFLOW_COMFYUI) colors.onPrimary else colors.textSecondary
                    )
                }
            }
        }

        // Render Active Studio Workspace
        AnimatedContent(
            targetState = studioMode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "studio_mode_switch"
        ) { mode ->
            when (mode) {
                StudioMode.QUICK_STUDIO -> QuickStudioView(viewModel = viewModel)
                StudioMode.NODE_WORKFLOW_COMFYUI -> NodeCanvasScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun QuickStudioView(viewModel: CompanionViewModel) {
    val colors = LocalKallistoColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val progressState by viewModel.imageProgressState.collectAsState()
    val sourceBitmap by viewModel.selectedSourceImage.collectAsState()
    val activeAspectRatio by viewModel.selectedAspectRatio.collectAsState()
    val baseResolution by viewModel.selectedBaseResolution.collectAsState()
    val upscaleMultiplier by viewModel.img2imgUpscaleMultiplier.collectAsState()
    val isSquareCrop by viewModel.forceSquareCrop.collectAsState()
    val strength by viewModel.img2imgStrength.collectAsState()

    var promptText by remember { mutableStateOf("") }
    var diffusionSteps by remember { mutableFloatStateOf(4f) }

    val isGenerating = progressState.state == ImageGenState.PREPROCESSING ||
            progressState.state == ImageGenState.DENOISING_STEPS ||
            progressState.state == ImageGenState.POSTPROCESSING

    LaunchedEffect(progressState.generatedBitmap) {
        if (progressState.generatedBitmap != null) {
            listState.animateScrollToItem(index = 2)
        }
    }

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

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 14.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Rounded.Brush, contentDescription = "Studio", tint = colors.primary, modifier = Modifier.size(20.dp))
                            Text(
                                text = if (sourceBitmap != null) "Img2Img & AI Upscale Studio" else "Text-to-Image Studio",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (sourceBitmap != null) "${(upscaleMultiplier * 100).toInt()}% Scale" else "${activeAspectRatio.label} • ${baseResolution}p",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accentWave
                            )
                        }
                    }
                    Text(
                        text = if (sourceBitmap != null) "Transform, stylize, or AI-upscale your photo with custom styles & resolution." else "Generate visuals from text with customizable aspect ratios and resolution tiers.",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (sourceBitmap != null) 140.dp else 100.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                        .clickable { photoPickerLauncher.launch("image/*") }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (sourceBitmap != null) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Image(
                                    bitmap = sourceBitmap!!.asImageBitmap(),
                                    contentDescription = "Source",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Column {
                                    Text(text = "Photo Loaded (${sourceBitmap!!.width}x${sourceBitmap!!.height})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                    Text(text = "Slide tools below for ratio, 1:1, or AI upscale", fontSize = 11.sp, color = colors.textSecondary)
                                }
                            }
                            IconButton(onClick = { viewModel.selectedSourceImage.value = null }) {
                                Icon(imageVector = Icons.Rounded.Cancel, contentDescription = "Clear", tint = colors.error)
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(imageVector = Icons.Rounded.AddPhotoAlternate, contentDescription = "Import", tint = colors.primary, modifier = Modifier.size(26.dp))
                            Column {
                                Text(text = "Tap to load photo for Img2Img & Upscale", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                Text(text = "Leave empty to generate from text prompt only", fontSize = 11.sp, color = colors.textSecondary)
                            }
                        }
                    }
                }
            }

            if (progressState.generatedBitmap != null || isGenerating) {
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
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isGenerating) progressState.stepDescription else "Result: ${progressState.outputDimensions}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "${(progressState.progressFraction * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accentWave
                            )
                        }

                        if (isGenerating) {
                            LinearProgressIndicator(
                                progress = { progressState.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = colors.primary,
                                trackColor = colors.surfaceVariant
                            )
                        }

                        if (progressState.generatedBitmap != null) {
                            Image(
                                bitmap = progressState.generatedBitmap!!.asImageBitmap(),
                                contentDescription = "Rendered Result",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(290.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.Black),
                                contentScale = ContentScale.Fit
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.statusSuccess.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = "Saved", tint = colors.statusSuccess, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Saved to Gallery in DCIM/KallistoAI",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.statusSuccess
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.sendMessage(
                                            userText = if (promptText.isNotBlank()) promptText else "Generated artwork (${progressState.outputDimensions})",
                                            sourceImage = null
                                        )
                                        Toast.makeText(context, "Sent to Chat!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Rounded.Send, contentDescription = "Chat", tint = colors.onPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Send to Chat", fontSize = 12.sp, color = colors.onPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (sourceBitmap != null) {
                        ToolTrayPill(label = "1:1 Square Lock", isSelected = isSquareCrop, onClick = { viewModel.forceSquareCrop.value = !isSquareCrop })

                        listOf(
                            "0.75x Scale" to 0.75f,
                            "1.0x Match" to 1.0f,
                            "1.5x HD Scale" to 1.5f,
                            "2.0x AI Upscale" to 2.0f
                        ).forEach { (label, multiplier) ->
                            ToolTrayPill(label = label, isSelected = upscaleMultiplier == multiplier, onClick = { viewModel.img2imgUpscaleMultiplier.value = multiplier })
                        }

                        listOf(
                            "30% Subtle" to 0.3f,
                            "60% Balanced" to 0.6f,
                            "85% Stylized" to 0.85f
                        ).forEach { (label, str) ->
                            ToolTrayPill(label = label, isSelected = strength == str, onClick = { viewModel.img2imgStrength.value = str })
                        }
                    } else {
                        AspectRatioOption.values().forEach { ratio ->
                            ToolTrayPill(label = ratio.label, isSelected = activeAspectRatio == ratio, onClick = { viewModel.selectedAspectRatio.value = ratio })
                        }

                        listOf(
                            "512p Fast" to 512,
                            "768p HD" to 768,
                            "1024p Z-Turbo" to 1024
                        ).forEach { (label, res) ->
                            ToolTrayPill(label = label, isSelected = baseResolution == res, onClick = { viewModel.selectedBaseResolution.value = res })
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(26.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (sourceBitmap != null) colors.primary.copy(alpha = 0.15f) else Color.Transparent)
                    ) {
                        Icon(imageVector = Icons.Rounded.AddPhotoAlternate, contentDescription = "Attach", tint = if (sourceBitmap != null) colors.primary else colors.textSecondary, modifier = Modifier.size(19.dp))
                    }

                    BasicTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.5.sp),
                        cursorBrush = SolidColor(colors.primary),
                        decorationBox = { innerTextField ->
                            if (promptText.isEmpty()) {
                                Text(
                                    text = if (sourceBitmap != null) "Style (e.g. 'cyberpunk', 'sketch', 'anime', 'oil painting')..." else "Prompt (e.g. 'futuristic orbital city, 8k')...",
                                    color = colors.textSecondary,
                                    fontSize = 13.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    val canGenerate = (promptText.isNotBlank() || sourceBitmap != null) && !isGenerating
                    IconButton(
                        onClick = {
                            if (canGenerate) {
                                coroutineScope.launch {
                                    viewModel.imageStudio.generateOrEditImage(
                                        prompt = promptText,
                                        inputImage = sourceBitmap,
                                        aspectRatio = activeAspectRatio,
                                        baseResolution = baseResolution,
                                        upscaleMultiplier = upscaleMultiplier,
                                        forceSquareCrop = isSquareCrop,
                                        strength = strength,
                                        steps = diffusionSteps.toInt()
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (canGenerate) colors.primary else colors.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = if (sourceBitmap != null) Icons.Rounded.AutoFixHigh else Icons.Rounded.Brush,
                            contentDescription = "Generate",
                            tint = if (canGenerate) colors.onPrimary else colors.textSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToolTrayPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalKallistoColors.current
    val bg = if (isSelected) colors.primary else colors.surface
    val textCol = if (isSelected) colors.onPrimary else colors.textSecondary
    val borderCol = if (isSelected) colors.primary else colors.border

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textCol
        )
    }
}
