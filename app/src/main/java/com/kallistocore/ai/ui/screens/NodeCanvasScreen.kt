package com.kallistocore.ai.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kallistocore.ai.domain.workflow.*
import com.kallistocore.ai.ui.theme.LocalKallistoColors
import com.kallistocore.ai.ui.viewmodel.CompanionViewModel
import kotlinx.coroutines.launch

@Composable
fun NodeCanvasScreen(viewModel: CompanionViewModel) {
    val colors = LocalKallistoColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val workflowEngine = remember { ComfyUiWorkflowEngine(context) }
    val comfyClient = viewModel.imageStudio.comfyClient

    var workflowGraph by remember { mutableStateOf(workflowEngine.createDefaultWorkflow()) }
    var activeExecutingNodeId by remember { mutableStateOf<String?>(null) }
    var isRunning by remember { mutableStateOf(false) }

    var serverUrlText by remember { mutableStateOf(comfyClient.serverUrl) }
    var isServerConnected by remember { mutableStateOf(false) }

    // Check server status
    LaunchedEffect(serverUrlText) {
        isServerConnected = comfyClient.checkServerConnection()
    }

    var canvasOffset by remember { mutableStateOf(Offset(0f, 0f)) }
    var canvasScale by remember { mutableFloatStateOf(1.0f) }

    val jsonPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    workflowGraph = workflowEngine.parseComfyUiJson(stream)
                }
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Infinite Grid & Wires
        Box(
            modifier = Modifier
                .fillMaxSize()
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

                for (connection in workflowGraph.connections) {
                    val fromNode = workflowGraph.nodes.find { it.id == connection.fromNodeId }
                    val toNode = workflowGraph.nodes.find { it.id == connection.toNodeId }

                    if (fromNode != null && toNode != null) {
                        val nodeWidth = 260f * canvasScale
                        val p1 = (fromNode.position + Offset(nodeWidth, 70f)) * canvasScale + canvasOffset
                        val p2 = (toNode.position + Offset(0f, 70f)) * canvasScale + canvasOffset

                        val dx = (p2.x - p1.x).coerceAtLeast(80f) * 0.5f
                        val path = Path().apply {
                            moveTo(p1.x, p1.y)
                            cubicTo(p1.x + dx, p1.y, p2.x - dx, p2.y, p2.x, p2.y)
                        }

                        drawPath(path = path, color = connection.portType.color.copy(alpha = 0.85f), style = Stroke(width = 3.5f * canvasScale, cap = StrokeCap.Round))
                    }
                }
            }

            workflowGraph.nodes.forEach { node ->
                val nodeOffset = node.position * canvasScale + canvasOffset
                val isExecuting = activeExecutingNodeId == node.id

                DraggableBentoNode(
                    node = node,
                    scale = canvasScale,
                    offset = nodeOffset,
                    isExecuting = isExecuting,
                    onMove = { delta ->
                        node.position += delta / canvasScale
                        workflowGraph = workflowGraph.copy()
                    },
                    onToggleCollapse = {
                        node.isCollapsed = !node.isCollapsed
                        workflowGraph = workflowGraph.copy()
                    }
                )
            }
        }

        // Top HUD & ComfyUI Server IP Config
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // ComfyUI Server IP Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .border(1.dp, if (isServerConnected) colors.statusSuccess else colors.border, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isServerConnected) colors.statusSuccess else colors.error)
                    )
                    Text(text = if (isServerConnected) "DGX Connected:" else "ComfyUI Server:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)

                    BasicTextField(
                        value = serverUrlText,
                        onValueChange = {
                            serverUrlText = it
                            comfyClient.serverUrl = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        cursorBrush = SolidColor(colors.primary)
                    )
                }

                Button(
                    onClick = { jsonPickerLauncher.launch("application/json") },
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.FileDownload, contentDescription = "Import", tint = colors.textPrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import JSON", fontSize = 11.sp, color = colors.textPrimary)
                }
            }
        }

        // Floating Bottom Execution Button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 70.dp, end = 20.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Button(
                onClick = {
                    if (!isRunning) {
                        isRunning = true
                        coroutineScope.launch {
                            val promptNode = workflowGraph.nodes.find { it.type == NodeType.CLIP_TEXT_ENCODE }
                            val promptText = promptNode?.params?.get("text") ?: "Cyberpunk city at neon midnight, 8k"

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
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) colors.accentWave else colors.primary)
            ) {
                Icon(imageVector = if (isRunning) Icons.Rounded.HourglassTop else Icons.Rounded.PlayArrow, contentDescription = "Run", tint = colors.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (isRunning) "Generating AI Image..." else "Queue Workflow Prompt", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.onPrimary)
            }
        }
    }
}

@Composable
fun DraggableBentoNode(
    node: CanvasNode,
    scale: Float,
    offset: Offset,
    isExecuting: Boolean,
    onMove: (delta: Offset) -> Unit,
    onToggleCollapse: () -> Unit
) {
    val colors = LocalKallistoColors.current
    val nodeWidth = 260.dp * scale

    Box(
        modifier = Modifier
            .offset(x = (offset.x / scale).dp, y = (offset.y / scale).dp)
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
                    .padding(horizontal = (12 * scale).dp, vertical = (8 * scale).dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((6 * scale).dp)) {
                    Box(modifier = Modifier.size((8 * scale).dp).clip(CircleShape).background(if (isExecuting) colors.statusSuccess else colors.primary))
                    Text(text = node.title, fontSize = (12 * scale).sp, fontWeight = FontWeight.Bold, color = colors.textPrimary, maxLines = 1)
                }

                IconButton(onClick = onToggleCollapse, modifier = Modifier.size((22 * scale).dp)) {
                    Icon(imageVector = if (node.isCollapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess, contentDescription = "Collapse", tint = colors.textSecondary, modifier = Modifier.size((16 * scale).dp))
                }
            }

            if (!node.isCollapsed) {
                Column(modifier = Modifier.padding((12 * scale).dp), verticalArrangement = Arrangement.spacedBy((8 * scale).dp)) {
                    node.inputs.forEach { port ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((6 * scale).dp)) {
                            Box(modifier = Modifier.size((10 * scale).dp).clip(CircleShape).background(port.type.color))
                            Text(text = port.name, fontSize = (10 * scale).sp, color = colors.textSecondary)
                        }
                    }

                    node.params.forEach { (key, value) ->
                        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape((8 * scale).dp)).background(colors.surfaceVariant.copy(alpha = 0.5f)).padding((6 * scale).dp)) {
                            Text(text = key.uppercase(), fontSize = (9 * scale).sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                            Text(text = value, fontSize = (11 * scale).sp, color = colors.textPrimary, maxLines = 2)
                        }
                    }

                    node.outputs.forEach { port ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = port.name, fontSize = (10 * scale).sp, color = colors.textSecondary)
                            Spacer(modifier = Modifier.width((6 * scale).dp))
                            Box(modifier = Modifier.size((10 * scale).dp).clip(CircleShape).background(port.type.color))
                        }
                    }
                }
            }
        }
    }
}
