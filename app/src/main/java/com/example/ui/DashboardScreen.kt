package com.example.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AutomationRule
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddRuleDialog by remember { mutableStateOf(false) }

    val rules by viewModel.allRules.collectAsStateWithLifecycle()
    val logs by viewModel.allLogs.collectAsStateWithLifecycle()
    val isSafeMode by viewModel.isSafeMode.collectAsStateWithLifecycle()
    
    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled.collectAsStateWithLifecycle()
    val isOverlayEnabled by viewModel.isOverlayEnabled.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "لوحة التحكم بالأتمتة الذكية",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    actions = {
                        // High-end Security Badge / Safe Mode Capsule
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .background(
                                    if (isSafeMode) Color(0xFF10B981).copy(alpha = 0.08f) else Color(0xFFEF4444).copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSafeMode) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isSafeMode) Icons.Default.Shield else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (isSafeMode) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSafeMode) "آمن" else "مفتوح",
                                fontSize = 11.sp,
                                color = if (isSafeMode) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isSafeMode,
                                onCheckedChange = { viewModel.toggleSafeMode() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF10B981),
                                    checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.3f),
                                    uncheckedThumbColor = Color(0xFFEF4444),
                                    uncheckedTrackColor = Color(0xFFEF4444).copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.scale(0.7f).height(20.dp)
                            )
                        }
                    }
                )
                // Thin Divider underneath TopAppBar (Cursor style)
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                )
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
                    label = { Text("المساعد الذكي", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Hub, contentDescription = "Workflows") },
                    label = { Text("مسارات العمل", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Build, contentDescription = "Rules") },
                    label = { Text("الأتمتة البسيطة", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Logs") },
                    label = { Text("سجل العمليات", fontSize = 10.sp) }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 2) {
                FloatingActionButton(
                    onClick = { showAddRuleDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.testTag("add_rule_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Rule")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Permissions Status Banner
            PermissionsBanner(
                isAccessibilityEnabled = isAccessibilityEnabled,
                isOverlayEnabled = isOverlayEnabled,
                onRequestAccessibility = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                },
                onRequestOverlay = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }
            )

            // Dynamic Tab Contents
            when (selectedTab) {
                0 -> ChatTabContent(viewModel)
                1 -> WorkflowBuilderScreen(viewModel)
                2 -> RulesTabContent(rules, onExecute = { viewModel.executeRuleDirectly(it) }, onDelete = { viewModel.deleteRule(it) })
                3 -> LogsTabContent(logs, onClear = { viewModel.clearHistory() })
            }
        }
    }

    if (showAddRuleDialog) {
        AddRuleDialog(
            onDismiss = { showAddRuleDialog = false },
            onConfirm = { name, triggerType, triggerValue, actionType, actionValue, desc ->
                viewModel.addNewRule(name, triggerType, triggerValue, actionType, actionValue, desc)
                showAddRuleDialog = false
            }
        )
    }
}

// --- Composable Subcomponents ---

@Composable
fun PermissionsBanner(
    isAccessibilityEnabled: Boolean,
    isOverlayEnabled: Boolean,
    onRequestAccessibility: () -> Unit,
    onRequestOverlay: () -> Unit
) {
    if (isAccessibilityEnabled && isOverlayEnabled) {
        // All permissions granted - show a highly elegant subtle status banner or hide to save space
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "تكوين النظام مطلوب للتشغيل التلقائي",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Accessibility Service Item
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                if (isAccessibilityEnabled) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFEF4444).copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAccessibilityEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = "Accessibility Status",
                            tint = if (isAccessibilityEnabled) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "خدمة الوصول الذكية (Accessibility)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "تسمح للوكيل بالضغط التلقائي وقراءة الشاشة",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                if (!isAccessibilityEnabled) {
                    Button(
                        onClick = onRequestAccessibility,
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("تفعيل", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                } else {
                    Text(
                        "مفعّل",
                        fontSize = 11.5.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            // 2. Overlay Permission Item
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                if (isOverlayEnabled) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFEF4444).copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOverlayEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = "Overlay Status",
                            tint = if (isOverlayEnabled) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "الظهور فوق التطبيقات (Overlay)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "تسمح برسم عناصر تحكم عائمة تفاعلية على الشاشة",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                if (!isOverlayEnabled) {
                    Button(
                        onClick = onRequestOverlay,
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("تفعيل", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                } else {
                    Text(
                        "مفعّل",
                        fontSize = 11.5.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatTabContent(viewModel: MainViewModel) {
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    var textState by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Smooth scroll to last message automatically on list change
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            if (chatMessages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        // AI Radiant Ring / Glowing Emblem
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF6366F1), // CyberIndigo
                                                Color(0xFF06B6D4)  // CyberCyan
                                            )
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Psychology,
                                    contentDescription = "Agent Emblem",
                                    modifier = Modifier.size(28.dp),
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            "الوكيل الذكي الفاخر",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            "اطلب من الذكاء الاصطناعي أتمتة هاتفك، فتح التطبيقات، أو صياغة مسارات عمل مرئية (Flows) بذكاء ثوري.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Suggested Quick Actions (ChatGPT style)
                        Text(
                            "اقتراحات سريعة للبدء:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.align(Alignment.Start).padding(start = 12.dp, bottom = 8.dp)
                        )

                        val quickSuggestions = listOf(
                            "صمم لي مسار الصباح التلقائي لتشغيل الـ TTS" to "🌅 مسار الصباح الذكي",
                            "ابنِ لي مساراً يحلل إشعارات الفواتير تلقائياً" to "🧾 فحص الإشعارات فورا",
                            "أتمتة فتح تطبيق الواتساب والتحقق من التنبيهات" to "📱 أتمتة تشغيل التطبيقات"
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            quickSuggestions.forEach { suggestion ->
                                Card(
                                    onClick = { textState = suggestion.first },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = suggestion.second,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            Icons.Default.ArrowForward,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(chatMessages) { msg ->
                        val isUser = msg.second
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                            verticalAlignment = Alignment.Top
                        ) {
                            if (!isUser) {
                                // Mini AI glowing avatar
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF6366F1), Color(0xFF06B6D4))
                                            ),
                                            shape = CircleShape
                                        )
                                        .align(Alignment.Top),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUser) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                                    }
                                ),
                                shape = if (isUser) {
                                    RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
                                } else {
                                    RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
                                },
                                border = if (!isUser) {
                                    androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                    )
                                } else null,
                                modifier = Modifier
                                    .widthIn(max = 280.dp)
                                    .shadow(
                                        elevation = if (isUser) 2.dp else 1.dp,
                                        shape = if (isUser) RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
                                    )
                            ) {
                                Text(
                                    text = msg.first,
                                    modifier = Modifier.padding(12.dp),
                                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }

                            if (isUser) {
                                Spacer(modifier = Modifier.width(8.dp))
                                // Mini User elegant placeholder
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            MaterialTheme.colorScheme.secondaryContainer,
                                            shape = CircleShape
                                        )
                                        .align(Alignment.Top),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Console Input Bar (Cursor style)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sparks / Assistant decoration icon
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(start = 12.dp, end = 4.dp)
                        .size(18.dp)
                )

                TextField(
                    value = textState,
                    onValueChange = { textState = it },
                    placeholder = {
                        Text(
                            "تحدث مع الوكيل أو أنشئ Flows برمجية...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                // High-End Glowing Send Button
                val isTyping = textState.isNotBlank()
                IconButton(
                    onClick = {
                        val cmd = textState
                        if (cmd.isNotBlank()) {
                            viewModel.sendChatCommand(cmd)
                            textState = ""
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isTyping) {
                                Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF06B6D4)))
                            } else {
                                Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            },
                            shape = CircleShape
                        )
                        .testTag("send_button")
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (isTyping) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RulesTabContent(
    rules: List<AutomationRule>,
    onExecute: (AutomationRule) -> Unit,
    onDelete: (AutomationRule) -> Unit
) {
    if (rules.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.SettingsApplications,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "لا توجد قواعد أتمتة مضافة حالياً.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "اضغط على الزر الدائري لإضافة قاعدة جديدة وأتمتة المهام اليومية.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rules) { rule ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    rule.name,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (rule.description.isNotBlank()) {
                                    Text(
                                        rule.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Run Rule button
                                IconButton(
                                    onClick = { onExecute(rule) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF10B981).copy(alpha = 0.1f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Run",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Delete Rule button
                                IconButton(
                                    onClick = { onDelete(rule) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFEF4444).copy(alpha = 0.1f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sleek Notion-style tag pills
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Trigger Tag
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "الشرط: ${rule.triggerType} (${rule.triggerValue})",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Action Tag
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Code,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "الإجراء: ${rule.actionType}",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
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
fun LogsTabContent(logs: List<com.example.data.LogEntry>, onClear: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "سجلات العمليات النشطة",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            if (logs.isNotEmpty()) {
                TextButton(
                    onClick = onClear,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                ) {
                    Text("مسح السجل", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
                }
            }
        }

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ListAlt,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "سجل الأتمتة فارغ.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Background Timeline vertical line connector
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = 13.dp)
                        .width(1.5.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(logs) { log ->
                        val date = Date(log.timestamp)
                        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(date)
                        
                        // Beautiful state matching colors
                        val statusColor = when (log.status.uppercase()) {
                            "SUCCESS" -> Color(0xFF10B981) // PremiumSuccess
                            "FAILED" -> Color(0xFFEF4444)  // PremiumError
                            else -> Color(0xFF6366F1)       // PremiumCyberIndigo
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Timeline bullet point with glowing outer halo
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(28.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(statusColor.copy(alpha = 0.15f), CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(statusColor, CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Log message card
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = log.actionName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = statusColor
                                        )
                                        Text(
                                            text = timeStr,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(2.dp))
                                    
                                    Text(
                                        text = log.message,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 15.sp
                                    )
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
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var triggerType by remember { mutableStateOf("TIME") }
    var triggerValue by remember { mutableStateOf("") }
    var actionType by remember { mutableStateOf("OPEN_APP") }
    var actionValue by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة قاعدة أتمتة جديدة", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("اسم القاعدة") })
                Spacer(modifier = Modifier.height(4.dp))
                TextField(value = description, onValueChange = { description = it }, label = { Text("الوصف") })
                Spacer(modifier = Modifier.height(4.dp))
                
                // Trigger Selection
                Text("نوع الشرط:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row {
                    RadioButton(selected = triggerType == "TIME", onClick = { triggerType = "TIME" })
                    Text("وقت مبرمج", fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterVertically))
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(selected = triggerType == "NOTIFICATION", onClick = { triggerType = "NOTIFICATION" })
                    Text("إشعار تطبيق", fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterVertically))
                }
                TextField(
                    value = triggerValue,
                    onValueChange = { triggerValue = it },
                    label = { Text(if (triggerType == "TIME") "الوقت المبرمج (مثل 08:30)" else "الكلمة المفتاحية للإشعار") }
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Action Selection
                Text("نوع الإجراء:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row {
                    RadioButton(selected = actionType == "OPEN_APP", onClick = { actionType = "OPEN_APP" })
                    Text("فتح تطبيق", fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterVertically))
                    Spacer(modifier = Modifier.width(4.dp))
                    RadioButton(selected = actionType == "TTS", onClick = { actionType = "TTS" })
                    Text("نطق صوتي", fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterVertically))
                }
                TextField(
                    value = actionValue,
                    onValueChange = { actionValue = it },
                    label = { Text(if (actionType == "OPEN_APP") "اسم الباقة (Package Name)" else "النص المراد نطقه") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, triggerType, triggerValue, actionType, actionValue, description)
                    }
                },
                modifier = Modifier.testTag("dialog_confirm_button")
            ) {
                Text("تأكيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
