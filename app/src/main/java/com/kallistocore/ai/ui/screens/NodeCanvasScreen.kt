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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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

    var workflowGraph by remember { mutableStateOf(workflowEngine.createDefaultWorkflow()) }
    var activeExecutingNodeId by remember { mutableStateOf<String?>(null) }
    var isRunning by remember { mutableStateOf(false) }

    // Canvas Pan & Zoom State
    var canvasOffset by remember { mutableStateOf(Offset(0f, 0f)) }
    var canvasScale by remember { mutableFloatStateOf(1.0f) }

    // ComfyUI JSON File Importer
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
        // 1. Infinite Visual Grid & Bezier Wires Canvas
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
            // Draw Dynamic Background Grid and Connection Wires
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridSpacing = 40f * canvasScale
                val startX = canvasOffset.x % gridSpacing
                val startY = canvasOffset.y % gridSpacing

                // Grid dots
                var x = startX
                while (x < size.width) {
                    var y = startY
                    while (y < size.height) {
                        drawCircle(
                            color = Color(0xFF334155).copy(alpha = 0.35f),
                            radius = 2f * canvasScale,
                            center = Offset(x, y)
                        )
                        y += gridSpacing
                    }
                    x += gridSpacing
                }

                // Draw Cubic Bezier Wire Cables between connected nodes
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

                        drawPath(
                            path = path,
                            color = connection.portType.color.copy(alpha = 0.85f),
                            style = Stroke(width = 3.5f * canvasScale, cap = StrokeCap.Round)
                        )
                    }
                }
            }

            // Render Draggable Bento Nodes
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
                        workflowGraph = workflowGraph.copy() // Trigger state recompose
                    },
                    onToggleCollapse = {
                        node.isCollapsed = !node.isCollapsed
                        workflowGraph = workflowGraph.copy()
                    }
                )
            }
        }

        // 2. Top Control HUD Bar (Import ComfyUI JSON, Reset, Zoom Indicator)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Rounded.AccountTree, contentDescription = "Nodes", tint = colors.accentWave, modifier = Modifier.size(18.dp))
                Text(
                    text = "${workflowGraph.nodes.size} Nodes • ${(canvasScale * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Import ComfyUI JSON Button
                Button(
                    onClick = { jsonPickerLauncher.launch("application/json") },
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceVariant)
                ) {
                    Icon(imageVector = Icons.Rounded.FileDownload, contentDescription = "Import", tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import ComfyUI JSON", fontSize = 11.5.sp, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                }

                // Reset Canvas Pan/Zoom
                IconButton(
                    onClick = {
                        canvasOffset = Offset(0f, 0f)
                        canvasScale = 1.0f
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceVariant)
                ) {
                    Icon(imageVector = Icons.Rounded.CenterFocusStrong, contentDescription = "Center", tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // 3. Floating Bottom Execution FAB
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
                Icon(
                    imageVector = if (isRunning) Icons.Rounded.HourglassTop else Icons.Rounded.PlayArrow,
                    contentDescription = "Run",
                    tint = colors.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "Executing ComfyUI Graph..." else "Queue Prompt Workflow",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimary
                )
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
            // Node Header (Draggable Handle)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((6 * scale).dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size((8 * scale).dp)
                            .clip(CircleShape)
                            .background(if (isExecuting) colors.statusSuccess else colors.primary)
                    )
                    Text(
                        text = node.title,
                        fontSize = (12 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = onToggleCollapse,
                    modifier = Modifier.size((22 * scale).dp)
                ) {
                    Icon(
                        imageVector = if (node.isCollapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                        contentDescription = "Collapse",
                        tint = colors.textSecondary,
                        modifier = Modifier.size((16 * scale).dp)
                    )
                }
            }

            // Node Body (Inputs, Parameters & Outputs)
            if (!node.isCollapsed) {
                Column(
                    modifier = Modifier.padding((12 * scale).dp),
                    verticalArrangement = Arrangement.spacedBy((8 * scale).dp)
                ) {
                    // Input Ports
                    node.inputs.forEach { port ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy((6 * scale).dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size((10 * scale).dp)
                                    .clip(CircleShape)
                                    .background(port.type.color)
                            )
                            Text(text = port.name, fontSize = (10 * scale).sp, color = colors.textSecondary)
                        }
                    }

                    // Node Parameters (Prompt text / Steps / Sampler)
                    node.params.forEach { (key, value) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape((8 * scale).dp))
                                .background(colors.surfaceVariant.copy(alpha = 0.5f))
                                .padding((6 * scale).dp)
                        ) {
                            Text(text = key.uppercase(), fontSize = (9 * scale).sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                            Text(text = value, fontSize = (11 * scale).sp, color = colors.textPrimary, maxLines = 2)
                        }
                    }

                    // Output Ports
                    node.outputs.forEach { port ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = port.name, fontSize = (10 * scale).sp, color = colors.textSecondary)
                            Spacer(modifier = Modifier.width((6 * scale).dp))
                            Box(
                                modifier = Modifier
                                    .size((10 * scale).dp)
                                    .clip(CircleShape)
                                    .background(port.type.color)
                            )
                        }
                    }
                }
            }
        }
    }
}
