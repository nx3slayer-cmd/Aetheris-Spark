package com.kallistocore.ai.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kallistocore.ai.data.manager.AppIconManager
import com.kallistocore.ai.data.manager.AppIconStyle
import com.kallistocore.ai.data.manager.ModelDownloadProgress
import com.kallistocore.ai.data.models.AIModelInfo
import com.kallistocore.ai.data.models.DownloadStatus
import com.kallistocore.ai.data.models.ModelCatalog
import com.kallistocore.ai.ui.theme.AppThemeSetting
import com.kallistocore.ai.ui.theme.LocalKallistoColors
import com.kallistocore.ai.ui.viewmodel.CompanionViewModel

@Composable
fun SettingsScreen(viewModel: CompanionViewModel) {
    val colors = LocalKallistoColors.current
    val context = LocalContext.current

    val currentTheme by viewModel.currentTheme.collectAsState()
    val downloadStates by viewModel.modelManager.downloadStates.collectAsState()
    val activeLlmId by viewModel.modelManager.activeLlmId.collectAsState()
    val activeImageId by viewModel.modelManager.activeImageModelId.collectAsState()
    val activeTtsId by viewModel.modelManager.activeTtsId.collectAsState()

    var activeIconStyle by remember { mutableStateOf(viewModel.settingsRepo.activeIcon) }
    var memoryAllocationMB by remember { mutableFloatStateOf(viewModel.settingsRepo.memoryAllocationMB.toFloat()) }
    var contextSize by remember { mutableFloatStateOf(viewModel.settingsRepo.contextWindowSize.toFloat()) }
    var cpuThreads by remember { mutableFloatStateOf(viewModel.settingsRepo.cpuThreads.toFloat()) }
    var systemPromptText by remember { mutableStateOf(viewModel.settingsRepo.systemPrompt) }
    var hfTokenText by remember { mutableStateOf(viewModel.modelManager.hfToken) }

    // Slider Confirmation Dialog State
    var pendingSettingChange by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    val freeStorage = viewModel.modelManager.formatBytes(viewModel.modelManager.getAvailableStorageBytes())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        // 1. Model Hub Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "On-Device Model Hub", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(text = "Free Disk: $freeStorage", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.accentWave)
            }
        }

        // 2. Hugging Face Access Token Bento
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Hugging Face Access Token", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Text(
                        text = "Get Token ↗",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentWave,
                        modifier = Modifier.clickable {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/settings/tokens"))
                            context.startActivity(browserIntent)
                        }
                    )
                }
                Text(text = "Optional. Paste your 'hf_...' token to download gated models directly.", fontSize = 11.sp, color = colors.textSecondary)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    BasicTextField(
                        value = hfTokenText,
                        onValueChange = {
                            hfTokenText = it
                            viewModel.modelManager.hfToken = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = colors.textPrimary, fontSize = 13.sp),
                        cursorBrush = SolidColor(colors.primary),
                        decorationBox = { innerTextField ->
                            if (hfTokenText.isEmpty()) {
                                Text(text = "hf_xxxxxxxxxxxxxxxxxxxxxxx (Optional)", color = colors.textSecondary, fontSize = 12.sp)
                            }
                            innerTextField()
                        }
                    )
                }
            }
        }

        // 3. Catalog Models
        items(ModelCatalog.curatedModels, key = { it.id }) { model ->
            val isActive = when (model.category) {
                com.kallistocore.ai.data.models.ModelCategory.CHAT_LLM -> activeLlmId == model.id
                com.kallistocore.ai.data.models.ModelCategory.IMAGE_GEN_AND_EDIT -> activeImageId == model.id
                com.kallistocore.ai.data.models.ModelCategory.VOICE_TTS -> activeTtsId == model.id
            }

            ModelHubCard(
                model = model,
                viewModel = viewModel,
                isActive = isActive,
                downloadProgress = downloadStates[model.id],
                onOpenHf = {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(model.hfRepoUrl))
                    context.startActivity(browserIntent)
                }
            )
        }

        // 4. App Theme Selector
        item {
            Text(text = "Aesthetic Theme", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Midnight" to AppThemeSetting.MIDNIGHT_DARK,
                    "Minimal" to AppThemeSetting.MINIMALIST_LIGHT,
                    "E-Ink (0%)" to AppThemeSetting.E_INK_BLACK,
                    "Nord" to AppThemeSetting.NORD_FOREST
                ).forEach { (name, theme) ->
                    val isSelected = currentTheme == theme
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) colors.primary else colors.surface)
                            .border(1.dp, if (isSelected) colors.primary else colors.border, RoundedCornerShape(14.dp))
                            .clickable {
                                viewModel.setTheme(theme)
                                viewModel.settingsRepo.theme = theme
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = name, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) colors.onPrimary else colors.textSecondary)
                    }
                }
            }
        }

        // 5. Launcher App Icon Selector
        item {
            Text(text = "Launcher App Icon Style", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppIconStyle.values().forEach { style ->
                    val isSelected = activeIconStyle == style
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) colors.surfaceVariant else colors.surface)
                            .border(1.dp, if (isSelected) colors.primary else colors.border, RoundedCornerShape(14.dp))
                            .clickable {
                                AppIconManager.setAppIcon(context, style)
                                activeIconStyle = style
                                viewModel.settingsRepo.activeIcon = style
                                Toast.makeText(context, "Applied ${style.displayName}!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(if (style.isProOnly) colors.accentWave else colors.primary))
                            Text(text = style.displayName, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = colors.textPrimary)
                        }
                        if (isSelected) {
                            Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = colors.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // 6. Memory Quota Slider with Confirmation Dialog
        item {
            Text(text = "Chat Memory Bank Allocation", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(text = "Dedicated Memory Quota", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text(text = "Persistent SQLite + associative vector cache", fontSize = 11.sp, color = colors.textSecondary)
                    }
                    Text(text = "${memoryAllocationMB.toInt()} MB (${String.format("%.1f", memoryAllocationMB / 1024.0)} GB)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                }

                Slider(
                    value = memoryAllocationMB,
                    onValueChange = { memoryAllocationMB = it },
                    onValueChangeFinished = {
                        pendingSettingChange = "Change Memory Allocation to ${memoryAllocationMB.toInt()} MB?" to {
                            viewModel.settingsRepo.memoryAllocationMB = memoryAllocationMB.toInt()
                            Toast.makeText(context, "Memory Quota Updated!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    valueRange = 512f..8192f,
                    steps = 14,
                    colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary, inactiveTrackColor = colors.surfaceVariant)
                )
            }
        }

        // 7. Hardware Allocations
        item {
            Text(text = "Hardware Allocations (12GB Phone Optimized)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Context Window Size", fontSize = 13.sp, color = colors.textPrimary)
                        Text("${contextSize.toInt()} Tokens", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                    }
                    Slider(
                        value = contextSize,
                        onValueChange = { contextSize = it },
                        onValueChangeFinished = {
                            pendingSettingChange = "Set Context Window to ${contextSize.toInt()} tokens?" to {
                                viewModel.settingsRepo.contextWindowSize = contextSize.toInt()
                                Toast.makeText(context, "Context Window Saved!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        valueRange = 2048f..8192f,
                        steps = 2,
                        colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary, inactiveTrackColor = colors.surfaceVariant)
                    )
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ARM64 CPU Cores", fontSize = 13.sp, color = colors.textPrimary)
                        Text("${cpuThreads.toInt()} Cores", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                    }
                    Slider(
                        value = cpuThreads,
                        onValueChange = { cpuThreads = it },
                        onValueChangeFinished = {
                            pendingSettingChange = "Allocate ${cpuThreads.toInt()} CPU cores for inference?" to {
                                viewModel.settingsRepo.cpuThreads = cpuThreads.toInt()
                                Toast.makeText(context, "CPU Thread Allocation Saved!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        valueRange = 2f..8f,
                        steps = 5,
                        colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary, inactiveTrackColor = colors.surfaceVariant)
                    )
                }
            }
        }
    }

    // Slider Confirmation Dialog
    pendingSettingChange?.let { (promptText, onConfirmAction) ->
        Dialog(onDismissRequest = { pendingSettingChange = null }) {
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .border(1.5.dp, colors.border, RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(text = "Confirm Setting Change", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(text = promptText, fontSize = 13.sp, color = colors.textSecondary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        // Revert slider back to saved repo value
                        memoryAllocationMB = viewModel.settingsRepo.memoryAllocationMB.toFloat()
                        contextSize = viewModel.settingsRepo.contextWindowSize.toFloat()
                        cpuThreads = viewModel.settingsRepo.cpuThreads.toFloat()
                        pendingSettingChange = null
                    }) {
                        Text("Cancel", color = colors.textSecondary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            onConfirmAction()
                            pendingSettingChange = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) {
                        Text("Apply & Save", color = colors.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun ModelHubCard(
    model: AIModelInfo,
    viewModel: CompanionViewModel,
    isActive: Boolean,
    downloadProgress: ModelDownloadProgress?,
    onOpenHf: () -> Unit
) {
    val colors = LocalKallistoColors.current
    val isDownloaded = viewModel.modelManager.isModelDownloaded(model)
    val isDownloading = downloadProgress?.status == DownloadStatus.DOWNLOADING
    val isFailed = downloadProgress?.status == DownloadStatus.FAILED
    val modelFile = viewModel.modelManager.getModelFile(model)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isActive) colors.surfaceVariant else colors.surface)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) colors.statusSuccess else colors.border,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = model.name, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.statusSuccess.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.statusSuccess)
                        }
                    }
                }
                Text(text = model.description, fontSize = 11.5.sp, color = colors.textSecondary, lineHeight = 15.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = model.formattedSize, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.accentWave)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Format: ${model.quantization} • RAM: ~${model.ramRequirementMB} MB", fontSize = 11.sp, color = colors.textSecondary)
            
            Text(
                text = "HF Repo ↗",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.accentWave,
                modifier = Modifier.clickable { onOpenHf() }
            )
        }

        if (isDownloaded) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.statusSuccess))
                Text("Installed on Device", fontSize = 11.sp, color = colors.statusSuccess, fontWeight = FontWeight.SemiBold)
            }
        }

        if (isFailed && downloadProgress?.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.error.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(text = downloadProgress.errorMessage, fontSize = 11.sp, color = colors.error, lineHeight = 15.sp)
            }
        }

        if (isDownloading && downloadProgress != null) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "${viewModel.modelManager.formatBytes(downloadProgress.downloadedBytes)} / ${viewModel.modelManager.formatBytes(downloadProgress.totalBytes)}",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                    Text(text = "${(downloadProgress.progress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                }
                LinearProgressIndicator(
                    progress = { downloadProgress.progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = colors.primary,
                    trackColor = colors.surfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isDownloaded) {
                Button(
                    onClick = { viewModel.modelManager.selectActiveModel(model) },
                    modifier = Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(10.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) colors.statusSuccess else colors.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Rounded.CheckCircle else Icons.Rounded.PowerSettingsNew,
                        contentDescription = "Active",
                        tint = colors.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (isActive) "Active Engine" else "Select as Active", fontSize = 12.sp, color = colors.onPrimary)
                }

                Button(
                    onClick = { viewModel.deleteModel(model) },
                    modifier = Modifier.height(38.dp).clip(RoundedCornerShape(10.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error.copy(alpha = 0.15f))
                ) {
                    Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Delete", tint = colors.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", fontSize = 12.sp, color = colors.error)
                }
            } else if (isDownloading) {
                Button(
                    onClick = { viewModel.modelManager.pauseOrCancelDownload(model) },
                    modifier = Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(10.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant)
                ) {
                    Icon(imageVector = Icons.Rounded.Pause, contentDescription = "Pause", tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pause / Background Download", fontSize = 12.sp, color = colors.textPrimary)
                }
            } else {
                val hasPartial = downloadProgress?.isResumable == true
                Button(
                    onClick = { viewModel.downloadModel(model) },
                    modifier = Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(10.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Icon(imageVector = if (hasPartial) Icons.Rounded.PlayArrow else Icons.Rounded.Download, contentDescription = "Download", tint = colors.onPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasPartial) "Resume Download (${(downloadProgress!!.progress * 100).toInt()}%)" else "Download Model (${model.formattedSize})",
                        fontSize = 12.sp,
                        color = colors.onPrimary
                    )
                }
            }
        }
    }
}
