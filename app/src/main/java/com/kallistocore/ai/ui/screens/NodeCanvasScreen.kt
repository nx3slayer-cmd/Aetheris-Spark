package com.kallistocore.ai.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kallistocore.ai.domain.workflow.*
import com.kallistocore.ai.ui.theme.LocalKallistoColors
import com.kallistocore.ai.ui.viewmodel.CompanionViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun NodeCanvasScreen(viewModel: CompanionViewModel) {
    val colors = LocalKallistoColors.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val workflowEngine = remember { ComfyUiWorkflowEngine(context) }
    val progressState by viewModel.imageProgressState.collectAsState()

    var workflowGraph by remember { mutableStateOf(workflowEngine.createDefaultWorkflow()) }
    var activeExecutingNodeId by remember { mutableStateOf<String?>(null) }
    var isRunning by remember { mutableStateOf(false) }

    var canvasOffset by remember { mutableStateOf(Offset(0f, 0f)) }
    var canvasScale by remember { mutableFloatStateOf(1.0f) }

    // Precision Port-to-Port Linking State
    var selectedOutputPort by remember { mutableStateOf<Triple<String, String, PortType>?>(null) } // (nodeId, portId, type)

    // Gesture State: Triple Tap for Node Search
    var tapCounter by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var showAddNodeModal by remember { mutableStateOf(false) }
    var spawnLocation by remember { mutableStateOf(Offset(200f, 200f)) }

    var showPresetMenu by remember { mutableStateOf(false) }
    var showSaveWorkflowDialog by remember { mutableStateOf(false) }
    var showLoadWorkflowDialog by remember { mutableStateOf(false) }

    val jsonPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    workflowGraph = workflowEngine.parseComfyUiJson(stream)
                    Toast.makeText(context, "Imported ComfyUI Workflow!", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // 1. Interactive Grid Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime < 350) {
                                tapCounter++
                            } else {
                                tapCounter = 1
                            }
                            lastTapTime = now

                            if (tapCounter >= 3) {
                                spawnLocation = (offset - canvasOffset) / canvasScale
                                showAddNodeModal = true
                                tapCounter = 0
                            } else {
                                selectedOutputPort = null // Clear wire selection on empty tap
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        canvasScale = (canvasScale * zoom).coerceIn(0.5f, 2.0f)
                        canvasOffset += pan
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridSpacing = 40f * canvasScale
                val startX = canvasOffset.x % gridSpacing
                val startY = canvasOffset.y % gridSpacing

                var x = startX
                while (x < size.width) {
                    var y = startY
                    while (y < size.height) {
                        drawCircle(color = Color(0xFF334155).copy(alpha = 0.35f), radius = 2f * canvasScale, center = Offset(x, y))
                        y += gridSpacing
                    }
                    x += gridSpacing
                }

                val nodeWidthPx = with(density) { (260.dp * canvasScale).toPx() }

                // Draw existing connections
                for (connection in workflowGraph.connections) {
                    val fromNode = workflowGraph.nodes.find { it.id == connection.fromNodeId }
                    val toNode = workflowGraph.nodes.find { it.id == connection.toNodeId }

                    if (fromNode != null && toNode != null) {
                        val p1 = Offset(
                            x = (fromNode.position.x * canvasScale) + canvasOffset.x + nodeWidthPx,
                            y = (fromNode.position.y * canvasScale) + canvasOffset.y + (65f * canvasScale)
                        )
                        val p2 = Offset(
                            x = (toNode.position.x * canvasScale) + canvasOffset.x,
                            y = (toNode.position.y * canvasScale) + canvasOffset.y + (65f * canvasScale)
                        )

                        val dx = (p2.x - p1.x).coerceAtLeast(60f * canvasScale) * 0.5f
                        val path = Path().apply {
                            moveTo(p1.x, p1.y)
                            cubicTo(p1.x + dx, p1.y, p2.x - dx, p2.y, p2.x, p2.y)
                        }

                        drawPath(path = path, color = connection.portType.color.copy(alpha = 0.9f), style = Stroke(width = 3.5f * canvasScale, cap = StrokeCap.Round))
                    }
                }
            }

            // Draggable Nodes with Tactile Port Taps
            workflowGraph.nodes.forEach { node ->
                val nodeOffset = Offset(
                    x = (node.position.x * canvasScale) + canvasOffset.x,
                    y = (node.position.y * canvasScale) + canvasOffset.y
                )
                val isExecuting = activeExecutingNodeId == node.id

                DraggableBentoNode(
                    node = node,
                    scale = canvasScale,
                    offset = nodeOffset,
                    isExecuting = isExecuting,
                    selectedOutputPort = selectedOutputPort,
                    onOutputPortTapped = { port ->
                        selectedOutputPort = Triple(node.id, port.id, port.type)
                        Toast.makeText(context, "Port ${port.name} selected. Tap an input port to link!", Toast.LENGTH_SHORT).show()
                    },
                    onInputPortTapped = { port ->
                        selectedOutputPort?.let { (fromNodeId, fromPortId, pType) ->
                            if (fromNodeId != node.id) {
                                val newConn = NodeWireConnection(UUID.randomUUID().toString(), fromNodeId, fromPortId, node.id, port.id, pType)
                                workflowGraph = workflowGraph.copy(connections = workflowGraph.connections + newConn)
                                selectedOutputPort = null
                                Toast.makeText(context, "Nodes Connected Successfully!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onMove = { delta ->
                        node.position += delta / canvasScale
                        workflowGraph = workflowGraph.copy()
                    },
                    onToggleCollapse = {
                        node.isCollapsed = !node.isCollapsed
                        workflowGraph = workflowGraph.copy()
                    },
                    onDeleteNode = {
                        workflowGraph = workflowGraph.copy(
                            nodes = workflowGraph.nodes.filter { it.id != node.id },
                            connections = workflowGraph.connections.filter { it.fromNodeId != node.id && it.toNodeId != node.id }
                        )
                    }
                )
            }
        }

        // 2. Compact Top HUD Bar with Job Presets Dropdown
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Job Presets Dropdown Pill
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                        .clickable { showPresetMenu = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.AccountTree, contentDescription = "Presets", tint = colors.accentWave, modifier = Modifier.size(16.dp))
                    Text(text = "Job Presets ▾", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                }

                DropdownMenu(
                    expanded = showPresetMenu,
                    onDismissRequest = { showPresetMenu = false }
                ) {
                    JobPreset.values().forEach { preset ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(preset.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(preset.description, fontSize = 11.sp, color = colors.textSecondary)
                                }
                            },
                            onClick = {
                                workflowGraph = workflowEngine.loadPresetWorkflow(preset)
                                showPresetMenu = false
                                Toast.makeText(context, "Loaded ${preset.displayName}!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // Compact Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = {
                        doubleTapLocation = Offset(200f, 200f)
                        showAddNodeModal = true
                    },
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(colors.primary)
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add", tint = colors.onPrimary, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = { showSaveWorkflowDialog = true },
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(colors.surfaceVariant)
                ) {
                    Icon(imageVector = Icons.Rounded.Save, contentDescription = "Save", tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = { showLoadWorkflowDialog = true },
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(colors.surfaceVariant)
                ) {
                    Icon(imageVector = Icons.Rounded.FolderOpen, contentDescription = "Load", tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = { jsonPickerLauncher.launch("application/json") },
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(colors.surfaceVariant)
                ) {
                    Icon(imageVector = Icons.Rounded.FileDownload, contentDescription = "Import", tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }

        // 3. Render Output Card
        if (progressState.generatedBitmap != null || isRunning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp, start = 14.dp, end = 14.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column(
                    modifier = Modifier
                        .width(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .border(1.5.dp, colors.accentWave, RoundedCornerShape(16.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = if (isRunning) "Synthesizing Output..." else "Output Preview", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        if (progressState.generatedBitmap != null) {
                            Text(text = "Saved to DCIM ✓", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.statusSuccess)
                        }
                    }

                    if (isRunning) {
                        LinearProgressIndicator(
                            progress = { progressState.progressFraction },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = colors.primary,
                            trackColor = colors.surfaceVariant
                        )
                    }

                    if (progressState.generatedBitmap != null) {
                        Image(
                            bitmap = progressState.generatedBitmap!!.asImageBitmap(),
                            contentDescription = "Output",
                            modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black),
                            contentScale = ContentScale.Fit
                        )

                        Button(
                            onClick = {
                                viewModel.sendMessage("Generated via ComfyUI workflow")
                                Toast.makeText(context, "Sent to Chat!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(8.dp)),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = colors.onPrimary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Send to Chat", fontSize = 11.sp, color = colors.onPrimary)
                        }
                    }
                }
            }
        }

        // 4. Queue Workflow Prompt Button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 20.dp, end = 16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Button(
                onClick = {
                    if (!isRunning) {
                        isRunning = true
                        coroutineScope.launch {
                            val promptNode = workflowGraph.nodes.find { it.type == NodeType.CLIP_TEXT_ENCODE }
                            val promptText = promptNode?.params?.get("text") ?: "A detailed photorealistic subject, 8k"

                            viewModel.imageStudio.generateOrEditImage(
                                prompt = promptText,
                                inputImage = viewModel.selectedSourceImage.value,
                                steps = 8
                            )

                            workflowEngine.executeWorkflow(workflowGraph) { runningNodeId ->
                                activeExecutingNodeId = runningNodeId
                            }
                            activeExecutingNodeId = null
                            isRunning = false
                        }
                    }
                },
                modifier = Modifier.height(44.dp).clip(RoundedCornerShape(14.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) colors.accentWave else colors.primary)
            ) {
                Icon(imageVector = if (isRunning) Icons.Rounded.HourglassTop else Icons.Rounded.PlayArrow, contentDescription = "Run", tint = colors.onPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = if (isRunning) "Running Graph..." else "Queue Workflow Prompt", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = colors.onPrimary)
            }
        }

        // 5. Triple-Tap Search & Add Node Modal
        if (showAddNodeModal) {
            SearchAndAddNodeDialog(
                onDismiss = { showAddNodeModal = false },
                onNodeSelected = { selectedType ->
                    val newNode = createNodeInstance(selectedType, spawnLocation)
                    workflowGraph = workflowGraph.copy(nodes = workflowGraph.nodes + newNode)
                    showAddNodeModal = false
                }
            )
        }

        // 6. Compact Save Workflow Dialog
        if (showSaveWorkflowDialog) {
            var wfName by remember { mutableStateOf("My_Custom_Workflow") }
            Dialog(onDismissRequest = { showSaveWorkflowDialog = false }) {
                Column(modifier = Modifier.width(300.dp).clip(RoundedCornerShape(18.dp)).background(colors.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Save Workflow", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(colors.surfaceVariant).padding(10.dp)) {
                        BasicTextField(value = wfName, onValueChange = { wfName = it }, modifier = Modifier.fillMaxWidth(), textStyle = TextStyle(color = colors.textPrimary, fontSize = 13.sp))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showSaveWorkflowDialog = false }) { Text("Cancel", color = colors.textSecondary) }
                        Button(onClick = {
                            workflowEngine.saveWorkflowToFile(workflowGraph, wfName)
                            showSaveWorkflowDialog = false
                            Toast.makeText(context, "Saved $wfName.json!", Toast.LENGTH_SHORT).show()
                        }) { Text("Save") }
                    }
                }
            }
        }

        // 7. Compact Load Workflow Dialog
        if (showLoadWorkflowDialog) {
            val savedFiles = workflowEngine.listSavedWorkflows()
            Dialog(onDismissRequest = { showLoadWorkflowDialog = false }) {
                Column(modifier = Modifier.width(300.dp).heightIn(max = 360.dp).clip(RoundedCornerShape(18.dp)).background(colors.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Load Saved Workflow", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    if (savedFiles.isEmpty()) {
                        Text("No saved workflows found.", fontSize = 12.sp, color = colors.textSecondary)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(savedFiles) { file ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(colors.surfaceVariant).clickable {
                                        file.inputStream().use { stream ->
                                            workflowGraph = workflowEngine.parseComfyUiJson(stream)
                                        }
                                        showLoadWorkflowDialog = false
                                        Toast.makeText(context, "Loaded ${file.name}!", Toast.LENGTH_SHORT).show()
                                    }.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(file.nameWithoutExtension, fontSize = 13.sp, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                                    Icon(imageVector = Icons.Rounded.ArrowForward, contentDescription = "Load", tint = colors.accentWave, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchAndAddNodeDialog(
    onDismiss: () -> Unit,
    onNodeSelected: (NodeType) -> Unit
) {
    val colors = LocalKallistoColors.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredNodes = NodeType.values().filter {
        it.displayName.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.width(320.dp).heightIn(max = 440.dp).clip(RoundedCornerShape(22.dp)).background(colors.surface).border(1.5.dp, colors.border, RoundedCornerShape(22.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Add ComfyUI Node", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }

            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.surfaceVariant).padding(horizontal = 10.dp, vertical = 8.dp)) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = colors.textPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(colors.primary),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text("Search (e.g. 'LoRA', 'KSampler', 'Image')...", color = colors.textSecondary, fontSize = 12.sp)
                        }
                        innerTextField()
                    }
                )
            }

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filteredNodes) { nodeType ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(colors.surfaceVariant.copy(alpha = 0.5f)).clickable { onNodeSelected(nodeType) }.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = nodeType.displayName, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                            Text(text = nodeType.category, fontSize = 10.sp, color = colors.accentWave)
                        }
                        Icon(imageVector = Icons.Rounded.AddCircleOutline, contentDescription = "Add", tint = colors.primary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

fun createNodeInstance(type: NodeType, position: Offset): CanvasNode {
    val id = UUID.randomUUID().toString().take(4)
    val inputs = type.defaultInputs.map { name -> NodePort("in_$name", name, mapPortType(name), true) }
    val outputs = type.defaultOutputs.map { name -> NodePort("out_$name", name, mapPortType(name), false) }

    val params = when (type) {
        NodeType.CHECKPOINT_LOADER -> mutableMapOf("ckpt_name" to "z_image_turbo-Q2_K.gguf")
        NodeType.LOAD_LORA -> mutableMapOf("lora_name" to "detail_tweaker.safetensors", "strength" to "1.0")
        NodeType.CLIP_TEXT_ENCODE -> mutableMapOf("text" to "A cinematic portrait in 8k, sharp focus")
        NodeType.KSAMPLER -> mutableMapOf("steps" to "8", "cfg" to "1.5", "denoise" to "0.65")
        NodeType.IMAGE_UPSCALE -> mutableMapOf("upscale_by" to "2.0")
        else -> mutableMapOf()
    }

    return CanvasNode(id = id, type = type, title = type.displayName, position = position, inputs = inputs, outputs = outputs, params = params)
}

fun mapPortType(name: String): PortType {
    val upper = name.uppercase()
    return when {
        upper.contains("MODEL") -> PortType.MODEL
        upper.contains("CLIP") -> PortType.CLIP
        upper.contains("LATENT") -> PortType.LATENT
        upper.contains("IMAGE") -> PortType.IMAGE
        upper.contains("VAE") -> PortType.VAE
        upper.contains("LORA") -> PortType.LORA
        else -> PortType.TEXT
    }
}

@Composable
fun DraggableBentoNode(
    node: CanvasNode,
    scale: Float,
    offset: Offset,
    isExecuting: Boolean,
    selectedOutputPort: Triple<String, String, PortType>?,
    onOutputPortTapped: (NodePort) -> Unit,
    onInputPortTapped: (NodePort) -> Unit,
    onMove: (delta: Offset) -> Unit,
    onToggleCollapse: () -> Unit,
    onDeleteNode: () -> Unit
) {
    val colors = LocalKallistoColors.current
    val density = LocalDensity.current

    val nodeWidth = with(density) { (260.dp * scale) }
    val offsetX = with(density) { offset.x.toDp() }
    val offsetY = with(density) { offset.y.toDp() }

    Box(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .width(nodeWidth)
            .clip(RoundedCornerShape((18 * scale).dp))
            .background(colors.surface)
            .border(
                width = if (isExecuting) (2.5 * scale).dp else (1 * scale).dp,
                color = if (isExecuting) colors.accentWave else colors.border,
                shape = RoundedCornerShape((18 * scale).dp)
            )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = (18 * scale).dp, topEnd = (18 * scale).dp))
                    .background(if (isExecuting) colors.accentWave.copy(alpha = 0.25f) else colors.surfaceVariant)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onMove(dragAmount)
                        }
                    }
                    .padding(horizontal = (10 * scale).dp, vertical = (6 * scale).dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((6 * scale).dp)) {
                    Box(modifier = Modifier.size((8 * scale).dp).clip(CircleShape).background(if (isExecuting) colors.statusSuccess else colors.primary))
                    Text(text = node.title, fontSize = (11.5 * scale).sp, fontWeight = FontWeight.Bold, color = colors.textPrimary, maxLines = 1)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDeleteNode, modifier = Modifier.size((20 * scale).dp)) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Delete", tint = colors.error, modifier = Modifier.size((14 * scale).dp))
                    }
                    IconButton(onClick = onToggleCollapse, modifier = Modifier.size((20 * scale).dp)) {
                        Icon(imageVector = if (node.isCollapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess, contentDescription = "Collapse", tint = colors.textSecondary, modifier = Modifier.size((16 * scale).dp))
                    }
                }
            }

            if (!node.isCollapsed) {
                Column(modifier = Modifier.padding((10 * scale).dp), verticalArrangement = Arrangement.spacedBy((6 * scale).dp)) {
                    // Input Ports with Tap Listener
                    node.inputs.forEach { port ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onInputPortTapped(port) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy((6 * scale).dp)
                        ) {
                            Box(modifier = Modifier.size((12 * scale).dp).clip(CircleShape).background(port.type.color))
                            Text(text = port.name, fontSize = (10 * scale).sp, color = colors.textSecondary)
                        }
                    }

                    // Node Parameters
                    node.params.forEach { (key, value) ->
                        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape((8 * scale).dp)).background(colors.surfaceVariant.copy(alpha = 0.5f)).padding((5 * scale).dp)) {
                            Text(text = key.uppercase(), fontSize = (8.5 * scale).sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                            Text(text = value, fontSize = (10.5 * scale).sp, color = colors.textPrimary, maxLines = 2)
                        }
                    }

                    // Output Ports with Highlight and Tap Listener
                    node.outputs.forEach { port ->
                        val isPortSelected = selectedOutputPort?.first == node.id && selectedOutputPort?.second == port.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOutputPortTapped(port) }
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = port.name, fontSize = (10 * scale).sp, color = if (isPortSelected) colors.accentWave else colors.textSecondary, fontWeight = if (isPortSelected) FontWeight.Bold else FontWeight.Normal)
                            Spacer(modifier = Modifier.width((6 * scale).dp))
                            Box(
                                modifier = Modifier
                                    .size((14 * scale).dp)
                                    .clip(CircleShape)
                                    .background(port.type.color)
                                    .border(if (isPortSelected) 2.dp else 0.dp, Color.White, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}
