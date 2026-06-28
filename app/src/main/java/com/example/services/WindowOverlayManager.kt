package com.example.services

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.ai.Content
import com.example.ai.GeminiService
import com.example.ai.Part
import com.example.data.AppDatabase
import com.example.data.AutomationRepository
import com.example.executor.CommandExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor")
class WindowOverlayManager(private val context: Context) : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null

    // Draggable position coordinates
    private var currentX = 100
    private var currentY = 300

    // Lifecycle variables to support Compose inside Overlay
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore = store

    init {
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun showOverlay() {
        if (overlayView != null) return

        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = currentX
            y = currentY
        }

        overlayView = ComposeView(context).apply {
            // Set owners
            setOwners(this@WindowOverlayManager, this@WindowOverlayManager, this@WindowOverlayManager)
            setContent {
                MaterialTheme {
                    OverlayContent()
                }
            }
        }

        windowManager.addView(overlayView, params)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun hideOverlay() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }

    private fun setOwners(
        lifecycleOwner: LifecycleOwner,
        savedStateOwner: SavedStateRegistryOwner,
        viewModelStoreOwner: ViewModelStoreOwner
    ) {
        overlayView?.let { view ->
            view.setViewTreeLifecycleOwner(lifecycleOwner)
            view.setViewTreeSavedStateRegistryOwner(savedStateOwner)
            view.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
        }
    }

    private fun updateFocus(focusable: Boolean) {
        params?.let { p ->
            if (focusable) {
                p.flags = p.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            } else {
                p.flags = p.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
            overlayView?.let { v ->
                windowManager.updateViewLayout(v, p)
            }
        }
    }

    @Composable
    fun OverlayContent() {
        var isExpanded by remember { mutableStateOf(false) }
        var inputCommand by remember { mutableStateOf("") }
        val messages = remember { mutableStateListOf<Pair<String, Boolean>>() } // text, isUser
        val coroutineScope = rememberCoroutineScope()
        
        val db = remember { AppDatabase.getDatabase(context) }
        val repository = remember { AutomationRepository(db) }
        val geminiService = remember { GeminiService() }
        val executor = remember { CommandExecutor(context, repository) }

        LaunchedEffect(isExpanded) {
            updateFocus(isExpanded)
        }

        Box(modifier = Modifier.wrapContentSize()) {
            if (!isExpanded) {
                // Collapsed Draggable Bubble
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                currentX += dragAmount.x.toInt()
                                currentY += dragAmount.y.toInt()
                                params?.let { p ->
                                    p.x = currentX
                                    p.y = currentY
                                    overlayView?.let { v ->
                                        windowManager.updateViewLayout(v, p)
                                    }
                                }
                            }
                        }
                        .clickable { isExpanded = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AI",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            } else {
                // Expanded Chat Screen
                Card(
                    modifier = Modifier
                        .width(320.dp)
                        .height(400.dp)
                        .padding(8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Title Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مساعد الأتمتة الذكي",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 14.sp
                            )
                            IconButton(onClick = { isExpanded = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }

                        // Messages list
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(8.dp),
                            reverseLayout = false
                        ) {
                            items(messages) { message ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = if (message.second) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (message.second) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = message.first,
                                            modifier = Modifier.padding(10.dp),
                                            color = if (message.second) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Input field and mic
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = inputCommand,
                                onValueChange = { inputCommand = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("اكتب أمراً للأتمتة...", fontSize = 12.sp) },
                                maxLines = 2,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )

                            IconButton(
                                onClick = {
                                    val command = inputCommand
                                    if (command.isNotBlank()) {
                                        messages.add(command to true)
                                        inputCommand = ""
                                        coroutineScope.launch {
                                            // Call Gemini
                                            val history = messages.map {
                                                Content(role = if (it.second) "user" else "model", parts = listOf(Part(text = it.first)))
                                            }
                                            repository.logInfo("GeminiAgent", "إرسال طلب أتمتة: '$command'")
                                            val response = geminiService.chatWithAgent(
                                                chatHistory = history,
                                                currentPrompt = command,
                                                screenContent = MyAccessibilityService.instance?.getScreenContentText()
                                            )

                                            val candidate = response?.candidates?.firstOrNull()
                                            val replyText = candidate?.content?.parts?.firstOrNull()?.text
                                            val functionCall = candidate?.content?.parts?.firstOrNull()?.functionCall

                                            if (functionCall != null) {
                                                messages.add("جاري تشغيل الدالة: ${functionCall.name}..." to false)
                                                // Execute function call
                                                val args = functionCall.args ?: emptyMap()
                                                val argVal = args.values.firstOrNull() ?: ""
                                                executor.executeAction(functionCall.name, argVal)
                                            } else if (replyText != null) {
                                                messages.add(replyText to false)
                                            } else {
                                                messages.add("عذراً، حدث خطأ أثناء الاتصال بـ Gemini." to false)
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                            }

                            IconButton(onClick = {
                                // Simulate Voice Recognition triggers
                                inputCommand = "شغل تطبيق الإعدادات"
                            }) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice", tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
