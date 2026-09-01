package com.kallistocore.ai.ui.screens

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kallistocore.ai.data.models.AIModelInfo
import com.kallistocore.ai.data.models.DownloadStatus
import com.kallistocore.ai.data.models.ModelCatalog
import com.kallistocore.ai.data.models.ModelCategory
import com.kallistocore.ai.ui.theme.AppThemeSetting
import com.kallistocore.ai.ui.theme.LocalKallistoColors
import com.kallistocore.ai.ui.viewmodel.CompanionViewModel

@Composable
fun SettingsScreen(viewModel: CompanionViewModel) {
    val colors = LocalKallistoColors.current

    val currentTheme by viewModel.currentTheme.collectAsState()
    val downloadStates by viewModel.modelManager.downloadStates.collectAsState()

    var memoryAllocationMB by remember { mutableFloatStateOf(1024f) }
    var contextSize by remember { mutableFloatStateOf(4096f) }
    var cpuThreads by remember { mutableFloatStateOf(6f) }
    var systemPromptText by remember { mutableStateOf(viewModel.systemPrompt.value) }

    val freeStorage = viewModel.modelManager.formatBytes(viewModel.modelManager.getAvailableStorageBytes())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        // App Theme Selector
        item {
            Text(
                text = "Aesthetic Theme",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
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
                            .clickable { viewModel.setTheme(theme) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) colors.onPrimary else colors.textSecondary
                        )
                    }
                }
            }
        }

        // Dedicated Chat Memory Bank Configuration (1 GB Default)
        item {
            Text(
                text = "Chat Memory Bank Allocation",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Dedicated Memory Quota",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Persistent FTS SQLite + associative vector cache",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }
                    Text(
                        text = "${memoryAllocationMB.toInt()} MB (${String.format("%.1f", memoryAllocationMB / 1024.0)} GB)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }

                Slider(
                    value = memoryAllocationMB,
                    onValueChange = { memoryAllocationMB = it },
                    valueRange = 512f..8192f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.primary,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = colors.surfaceVariant
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { viewModel.pruneMemoryBank() },
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
                    ) {
                        Icon(imageVector = Icons.Rounded.CleaningServices, contentDescription = "Prune", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Prune Old Memories", fontSize = 12.sp)
                    }
                }
            }
        }

        // Model Hub & Storage Manager Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "On-Device Model Hub",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Text(
                    text = "Free Disk: $freeStorage",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.accentWave
                )
            }
        }

        // Catalog Model Items
        items(ModelCatalog.curatedModels, key = { it.id }) { model ->
            ModelHubCard(
                model = model,
                viewModel = viewModel,
                downloadProgress = downloadStates[model.id]
            )
        }

        // Hardware & Engine Settings
        item {
            Text(
                text = "Hardware & Context Allocations",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Context Window
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Context Window Size", fontSize = 13.sp, color = colors.textPrimary)
                        Text("${contextSize.toInt()} Tokens", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                    }
                    Slider(
                        value = contextSize,
                        onValueChange = { contextSize = it },
                        valueRange = 2048f..8192f,
                        steps = 2,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.primary,
                            activeTrackColor = colors.primary,
                            inactiveTrackColor = colors.surfaceVariant
                        )
                    )
                }

                // CPU Threads
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ARM64 CPU Threads", fontSize = 13.sp, color = colors.textPrimary)
                        Text("${cpuThreads.toInt()} Cores", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                    }
                    Slider(
                        value = cpuThreads,
                        onValueChange = { cpuThreads = it },
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

        // System Prompt & Persona
        item {
            Text(
                text = "System Prompt & Persona",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                BasicTextField(
                    value = systemPromptText,
                    onValueChange = {
                        systemPromptText = it
                        viewModel.systemPrompt.value = it
                    },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = TextStyle(
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    cursorBrush = SolidColor(colors.primary)
                )
            }
        }
    }
}

@Composable
fun ModelHubCard(
    model: AIModelInfo,
    viewModel: CompanionViewModel,
    downloadProgress: com.kallistocore.ai.data.manager.ModelDownloadProgress?
) {
    val colors = LocalKallistoColors.current
    val isDownloaded = viewModel.modelManager.isModelDownloaded(model)
    val isDownloading = downloadProgress?.status == DownloadStatus.DOWNLOADING
    val modelFile = viewModel.modelManager.getModelFile(model)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Model Title & Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.name,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = model.description,
                    fontSize = 11.5.sp,
                    color = colors.textSecondary,
                    lineHeight = 15.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = model.formattedSize,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accentWave
                )
            }
        }

        // Storage Path & RAM details
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Format: ${model.format.name} (${model.quantization}) • RAM: ~${model.ramRequirementMB} MB",
                fontSize = 11.sp,
                color = colors.textSecondary
            )
            if (isDownloaded) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.statusSuccess))
                    Text("Downloaded", fontSize = 11.sp, color = colors.statusSuccess, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // File Path location
        Text(
            text = "Path: ${modelFile.parentFile?.name}/${model.fileName}",
            fontSize = 10.sp,
            color = colors.textSecondary.copy(alpha = 0.7f)
        )

        // Download Progress Bar
        if (isDownloading && downloadProgress != null) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Downloading chunk stream...", fontSize = 11.sp, color = colors.textSecondary)
                    Text("${(downloadProgress.progress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                }
                LinearProgressIndicator(
                    progress = { downloadProgress.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = colors.primary,
                    trackColor = colors.surfaceVariant,
                )
            }
        }

        // Actions: Download / Delete / Active selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isDownloaded) {
                Button(
                    onClick = { viewModel.modelManager.autoSelectActiveModel(model) },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("Select as Active", fontSize = 12.sp, color = colors.onPrimary)
                }

                Button(
                    onClick = { viewModel.deleteModel(model) },
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error.copy(alpha = 0.15f))
                ) {
                    Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Delete", tint = colors.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", fontSize = 12.sp, color = colors.error)
                }
            } else {
                Button(
                    onClick = { viewModel.downloadModel(model) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    enabled = !isDownloading,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Icon(imageVector = Icons.Rounded.Download, contentDescription = "Download", tint = colors.onPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isDownloading) "Downloading..." else "Download Model (${model.formattedSize})", fontSize = 12.sp, color = colors.onPrimary)
                }
            }
        }
    }
}
