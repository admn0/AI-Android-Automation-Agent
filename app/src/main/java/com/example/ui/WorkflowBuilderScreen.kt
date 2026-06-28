package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Workflow
import com.example.data.WorkflowConnection
import com.example.data.WorkflowNode
import com.example.viewmodel.MainViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowBuilderScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedWf by viewModel.selectedWorkflow.collectAsState()
    val workflows by viewModel.allWorkflows.collectAsState()
    val nodes by viewModel.activeNodes.collectAsState()
    val connections by viewModel.activeConnections.collectAsState()

    var showAddNodeDialog by remember { mutableStateOf(false) }
    var showConnectDialog by remember { mutableStateOf(false) }
    var showCreateWfDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Dropdown States
    var workflowsDropdownExpanded by remember { mutableStateOf(false) }

    // Canvas Transform state (Zoom & Pan)
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 2.5f)
        offset += offsetChange * scale
    }

    // Infinite transition for flowing energy dot along curves
    val infiniteTransition = rememberInfiniteTransition(label = "energy_flow")
    val flowProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flowProgress"
    )

    Scaffold(
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Connect tool
                FloatingActionButton(
                    onClick = { showConnectDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.Link, contentDescription = "ربط العقد")
                }
                // Add node tool
                FloatingActionButton(
                    onClick = { showAddNodeDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة عقدة")
                }
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top Control Ribbon (Workspace Selector & Run Triggers)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Dropdown
                    Box {
                        Button(
                            onClick = { workflowsDropdownExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        ) {
                            Text(
                                text = selectedWf?.name ?: "اختر مسار عمل",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 140.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = workflowsDropdownExpanded,
                            onDismissRequest = { workflowsDropdownExpanded = false }
                        ) {
                            workflows.forEach { wf ->
                                DropdownMenuItem(
                                    text = { Text(wf.name, fontSize = 14.sp) },
                                    onClick = {
                                        viewModel.selectWorkflow(wf)
                                        workflowsDropdownExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                                text = { Text("مسار عمل جديد", color = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showCreateWfDialog = true
                                    workflowsDropdownExpanded = false
                                }
                            )
                        }
                    }

                    // Ribbon Actions (Run, Clean, Delete)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف المسار الحالي")
                        }
                        IconButton(
                            onClick = { viewModel.deleteConnectionsForActiveWorkflow() },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(Icons.Default.LinkOff, contentDescription = "فك كافة الروابط")
                        }
                        Button(
                            onClick = { viewModel.executeActiveWorkflowDirectly() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تشغيل", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Description banner
            selectedWf?.let { wf ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = wf.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Dynamic Interactive Dotted Canvas Space
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
                    .transformable(state = state)
            ) {
                // 1. Grid Canvas Background (draw dotted grid)
                val gridColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val dotSpacing = 36f * scale
                    val startX = offset.x % dotSpacing
                    val startY = offset.y % dotSpacing

                    var x = startX
                    while (x < size.width) {
                        var y = startY
                        while (y < size.height) {
                            drawCircle(
                                color = gridColor,
                                radius = 1.5f * scale,
                                center = Offset(x, y)
                            )
                            y += dotSpacing
                        }
                        x += dotSpacing
                    }
                }

                // 2. Connection Lines (Cubic curves with animated energy flow)
                val connectionColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                val glowColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                val secondaryColor = MaterialTheme.colorScheme.secondary
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                ) {
                    connections.forEach { conn ->
                        val fromNode = nodes.firstOrNull { it.id == conn.fromNodeId }
                        val toNode = nodes.firstOrNull { it.id == conn.toNodeId }
                        if (fromNode != null && toNode != null) {
                            // Target coordinate offsets (Card centers / ports)
                            val fromX = fromNode.x + 180f // Right port of Card
                            val fromY = fromNode.y + 60f
                            val toX = toNode.x // Left port of Card
                            val toY = toNode.y + 60f

                            val path = Path().apply {
                                moveTo(fromX, fromY)
                                cubicTo(
                                    x1 = fromX + 100f, y1 = fromY,
                                    x2 = toX - 100f, y2 = toY,
                                    x3 = toX, y3 = toY
                                )
                            }

                            // Draw subtle path background glow
                            drawPath(
                                path = path,
                                color = glowColor,
                                style = Stroke(width = 6f, cap = StrokeCap.Round)
                            )

                            // Draw main sharp connection path
                            drawPath(
                                path = path,
                                color = connectionColor,
                                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                            )

                            // Draw animated flowing energy dot on the Bezier curve
                            val t = flowProgress
                            val xT = (1-t)*(1-t)*(1-t)*fromX + 3*(1-t)*(1-t)*t*(fromX + 100f) + 3*(1-t)*t*t*(toX - 100f) + t*t*t*toX
                            val yT = (1-t)*(1-t)*(1-t)*fromY + 3*(1-t)*(1-t)*t*fromY + 3*(1-t)*t*t*toY + t*t*t*toY

                            // Outer glow
                            drawCircle(
                                color = secondaryColor,
                                radius = 6f,
                                center = Offset(xT, yT)
                            )
                            // Inner core
                            drawCircle(
                                color = Color.White,
                                radius = 3.5f,
                                center = Offset(xT, yT)
                            )
                        }
                    }
                }

                // 3. Floating Interactive Nodes
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                ) {
                    nodes.forEach { node ->
                        WorkflowNodeCard(
                            node = node,
                            onNodeDrag = { deltaX, deltaY ->
                                viewModel.updateNodePosition(
                                    node.id,
                                    node.x + deltaX,
                                    node.y + deltaY
                                )
                            },
                            onDeleteClick = {
                                viewModel.deleteNodeFromActiveWorkflow(node.id)
                            }
                        )
                    }
                }

                // 4. Mini Map Overlay (Sleek Glassmorphic minimap)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .size(110.dp, 80.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val mapScale = 0.08f
                        // Draw mini active nodes with elegant circles
                        nodes.forEach { node ->
                            val tinyX = (node.x * mapScale).coerceIn(4f, size.width - 16f)
                            val tinyY = (node.y * mapScale).coerceIn(4f, size.height - 12f)
                            drawCircle(
                                color = when (node.type.uppercase()) {
                                    "TRIGGER_TIME", "TRIGGER_NOTIFICATION" -> Color(0xFFEF6C00)
                                    "AI_AGENT" -> Color(0xFF8B5CF6)
                                    "DELAY" -> Color(0xFF64748B)
                                    else -> Color(0xFF6366F1)
                                },
                                radius = 3.5f,
                                center = Offset(tinyX, tinyY)
                            )
                        }
                    }
                    Text(
                        "خريطة مصغرة",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 3.dp)
                    )
                }
            }
        }
    }

    // CREATE WORKFLOW DIALOG
    if (showCreateWfDialog) {
        var newWfName by remember { mutableStateOf("") }
        var newWfDesc by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showCreateWfDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("مسار عمل جديد", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = newWfName,
                        onValueChange = { newWfName = it },
                        label = { Text("اسم المسار") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newWfDesc,
                        onValueChange = { newWfDesc = it },
                        label = { Text("وصف مسار الأتمتة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showCreateWfDialog = false }) {
                            Text("إلغاء")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newWfName.isNotBlank()) {
                                    viewModel.addNewWorkflow(newWfName, newWfDesc)
                                    showCreateWfDialog = false
                                }
                            }
                        ) {
                            Text("إنشاء")
                        }
                    }
                }
            }
        }
    }

    // ADD NODE DIALOG
    if (showAddNodeDialog) {
        var nodeType by remember { mutableStateOf("TRIGGER_TIME") }
        var nodeLabel by remember { mutableStateOf("") }
        var configValue by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddNodeDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("إضافة عقدة لمسار الأتمتة", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)

                    Text("اختر نوع العقدة:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    val types = listOf(
                        "TRIGGER_TIME" to "مؤقت وقت (مثلا 08:30)",
                        "TRIGGER_NOTIFICATION" to "لاقط إشعارات (مثلا فاتورة)",
                        "AI_AGENT" to "وكيل ذكاء اصطناعي AI",
                        "DELAY" to "انتظار (مللي ثانية)",
                        "HTTP_REQUEST" to "طلب HTTP GET",
                        "ACTION_TTS" to "نطق ترحيب صوتي TTS",
                        "ACTION_OPEN_APP" to "تشغيل تطبيق برمجياً",
                        "ACTION_SMS" to "إرسال رسالة SMS"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(types) { item ->
                            FilterChip(
                                selected = nodeType == item.first,
                                onClick = { 
                                    nodeType = item.first
                                    nodeLabel = item.second.split(" ").first()
                                },
                                label = { Text(item.second, fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = nodeLabel,
                        onValueChange = { nodeLabel = it },
                        label = { Text("عنوان العقدة") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = configValue,
                        onValueChange = { configValue = it },
                        label = { Text("معلمة التكوين (التفاصيل/القيمة)") },
                        placeholder = { Text("مثال: 08:30 أو com.whatsapp") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddNodeDialog = false }) {
                            Text("إلغاء")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (nodeLabel.isNotBlank()) {
                                    viewModel.addNodeToActiveWorkflow(nodeType, nodeLabel, configValue)
                                    showAddNodeDialog = false
                                }
                            }
                        ) {
                            Text("إضافة")
                        }
                    }
                }
            }
        }
    }

    // CONNECT NODES DIALOG
    if (showConnectDialog) {
        var fromNodeId by remember { mutableStateOf<Int?>(null) }
        var toNodeId by remember { mutableStateOf<Int?>(null) }

        Dialog(onDismissRequest = { showConnectDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("ربط العقد معاً", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)

                    Text("من العقدة (المصدر):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Box {
                        var expandedFrom by remember { mutableStateOf(false) }
                        Button(onClick = { expandedFrom = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(nodes.firstOrNull { it.id == fromNodeId }?.label ?: "اختر عقدة المصدر")
                        }
                        DropdownMenu(expanded = expandedFrom, onDismissRequest = { expandedFrom = false }) {
                            nodes.forEach { n ->
                                DropdownMenuItem(
                                    text = { Text(n.label) },
                                    onClick = {
                                        fromNodeId = n.id
                                        expandedFrom = false
                                    }
                                )
                            }
                        }
                    }

                    Text("إلى العقدة (الهدف):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Box {
                        var expandedTo by remember { mutableStateOf(false) }
                        Button(onClick = { expandedTo = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(nodes.firstOrNull { it.id == toNodeId }?.label ?: "اختر العقدة المستهدفة")
                        }
                        DropdownMenu(expanded = expandedTo, onDismissRequest = { expandedTo = false }) {
                            nodes.forEach { n ->
                                DropdownMenuItem(
                                    text = { Text(n.label) },
                                    onClick = {
                                        toNodeId = n.id
                                        expandedTo = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showConnectDialog = false }) {
                            Text("إلغاء")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (fromNodeId != null && toNodeId != null) {
                                    viewModel.connectNodesInActiveWorkflow(fromNodeId!!, toNodeId!!)
                                    showConnectDialog = false
                                }
                            }
                        ) {
                            Text("ربط")
                        }
                    }
                }
            }
        }
    }

    // DELETE WORKFLOW CONFIRM DIALOG
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("حذف مسار الأتمتة") },
            text = { Text("هل أنت متأكد من رغبتك في حذف مسار العمل بالكامل؟ سيؤدي ذلك لحذف العقد والروابط بالكامل.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteActiveWorkflow()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun WorkflowNodeCard(
    node: WorkflowNode,
    onNodeDrag: (Float, Float) -> Unit,
    onDeleteClick: () -> Unit
) {
    // Specific colors based on node category
    val primaryColor = when (node.type.uppercase()) {
        "TRIGGER_TIME", "TRIGGER_NOTIFICATION" -> Color(0xFFF59E0B) // Radiant Orange/Amber Triggers
        "AI_AGENT" -> Color(0xFF8B5CF6) // Premium CyberPurple AI Agent
        "DELAY" -> Color(0xFF64748B) // Sleek Slate Delay
        "HTTP_REQUEST" -> Color(0xFF06B6D4) // Webhook Cyan
        else -> Color(0xFF6366F1) // Modern Indigo actions
    }

    val icon = when (node.type.uppercase()) {
        "TRIGGER_TIME" -> Icons.Default.Timer
        "TRIGGER_NOTIFICATION" -> Icons.Default.NotificationsActive
        "AI_AGENT" -> Icons.Default.Psychology
        "DELAY" -> Icons.Default.HourglassEmpty
        "HTTP_REQUEST" -> Icons.Default.CloudQueue
        "ACTION_TTS" -> Icons.Default.VolumeUp
        "ACTION_OPEN_APP" -> Icons.Default.Launch
        else -> Icons.Default.PlayArrow
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(node.x.roundToInt(), node.y.roundToInt()) }
            .size(180.dp, 120.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onNodeDrag(dragAmount.x, dragAmount.y)
                }
            }
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                RoundedCornerShape(16.dp)
            )
            .border(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        primaryColor,
                        primaryColor.copy(alpha = 0.25f)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = primaryColor.copy(alpha = 0.35f),
                spotColor = primaryColor.copy(alpha = 0.35f)
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row (Icon & Title & Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(primaryColor.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = node.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(95.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(22.dp)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "مسح العقدة",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            // Body Config String Preview
            Text(
                text = node.configValue,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 6.dp)
            )

            // Category tag (Sleek pill)
            Box(
                modifier = Modifier
                    .background(primaryColor.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .align(Alignment.End)
            ) {
                Text(
                    text = node.type,
                    fontSize = 7.5.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Port Circles representation (Glowing visual sockets)
        // Input port (left)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-16).dp)
                .size(10.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .border(2.dp, primaryColor, CircleShape)
        )

        // Output port (right)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 16.dp)
                .size(10.dp)
                .background(primaryColor, CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                .shadow(elevation = 2.dp, shape = CircleShape, spotColor = primaryColor)
        )
    }
}
