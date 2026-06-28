package com.example.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.AppDatabase
import com.example.data.AutomationRepository
import com.example.data.LogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: MyAccessibilityService? = null
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var repository: AutomationRepository

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val db = AppDatabase.getDatabase(this)
        repository = AutomationRepository(db)
        
        serviceScope.launch {
            repository.insertLog(
                LogEntry(
                    actionName = "AccessibilityService",
                    status = "SUCCESS",
                    message = "تم تفعيل خدمة الوصول بنجاح والاتصال بالنظام."
                )
            )
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Capture notification triggers or system events
        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            val parcelable = event.parcelableData
            // Can extract notification messages to feed into Automation Trigger Engine
        }
    }

    override fun onInterrupt() {
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    // --- Core Screen Parsing and Element Retrieval ---

    fun getScreenContentText(): String {
        val root = rootInActiveWindow ?: return "لا توجد عناصر ظاهرة على الشاشة حالياً."
        val builder = StringBuilder()
        parseNodeTree(root, builder, 0)
        return builder.toString()
    }

    private fun parseNodeTree(node: AccessibilityNodeInfo?, builder: StringBuilder, depth: Int) {
        if (node == null) return
        val text = node.text?.toString() ?: node.contentDescription?.toString()
        val viewId = node.viewIdResourceName
        
        if (!text.isNullOrBlank() || !viewId.isNullOrBlank()) {
            val indent = "  ".repeat(depth)
            val cleanId = viewId?.substringAfter("id/") ?: ""
            val type = node.className?.toString()?.substringAfterLast(".") ?: "Element"
            
            builder.append("$indent$type[")
            if (cleanId.isNotEmpty()) builder.append("id=$cleanId, ")
            if (!text.isNullOrBlank()) builder.append("text='$text', ")
            builder.append("clickable=${node.isClickable}]\n")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                parseNodeTree(child, builder, depth + 1)
            }
        }
    }

    // --- Core Action Simulation ---

    fun clickElement(target: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNodeByTextOrId(root, target)
        if (node != null) {
            var clickableNode: AccessibilityNodeInfo? = node
            while (clickableNode != null && !clickableNode.isClickable) {
                clickableNode = clickableNode.parent
            }
            return if (clickableNode != null && clickableNode.isClickable) {
                clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            } else {
                // If it's not clickable, click via coordinates
                clickAtCoordinates(node)
            }
        }
        return false
    }

    fun inputTextField(target: String, text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNodeByTextOrId(root, target) ?: return false
        
        var editableNode: AccessibilityNodeInfo? = node
        while (editableNode != null && !editableNode.isEditable) {
            editableNode = editableNode.parent
        }
        
        val targetNode = editableNode ?: node
        
        // Focus first
        targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun findNodeByTextOrId(node: AccessibilityNodeInfo?, target: String): AccessibilityNodeInfo? {
        if (node == null) return null
        
        val text = node.text?.toString() ?: node.contentDescription?.toString()
        val viewId = node.viewIdResourceName?.substringAfter("id/") ?: ""
        
        // Match exact or partial/contains
        if (text?.equals(target, ignoreCase = true) == true || 
            viewId.equals(target, ignoreCase = true) || 
            (text != null && text.contains(target, ignoreCase = true))
        ) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val found = findNodeByTextOrId(child, target)
                if (found != null) return found
            }
        }
        return null
    }

    fun clickCoordinates(x: Float, y: Float): Boolean {
        return swipe(x, y, x, y, 50)
    }

    fun longClickCoordinates(x: Float, y: Float, durationMs: Long = 1000): Boolean {
        return swipe(x, y, x, y, durationMs)
    }

    private fun clickAtCoordinates(node: AccessibilityNodeInfo): Boolean {
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        val x = bounds.centerX().toFloat()
        val y = bounds.centerY().toFloat()
        return swipe(x, y, x, y, 50)
    }

    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long): Boolean {
        val stroke = GestureDescription.StrokeDescription(
            Path().apply {
                moveTo(startX, startY)
                lineTo(endX, endY)
            },
            0, durationMs
        )
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    fun scrollForward(target: String? = null): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = if (target != null) findNodeByTextOrId(root, target) else root
        return node?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) ?: false
    }

    fun scrollBackward(target: String? = null): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = if (target != null) findNodeByTextOrId(root, target) else root
        return node?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) ?: false
    }

    fun performGlobalBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun performGlobalHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }
}
