package com.example.executor

import android.content.Context
import android.util.Log
import com.example.ai.Content
import com.example.ai.GeminiService
import com.example.ai.Part
import com.example.data.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale

class WorkflowExecutor(
    private val context: Context,
    private val repository: AutomationRepository
) {
    private val client = OkHttpClient()
    private val commandExecutor = CommandExecutor(context, repository)
    private val geminiService = GeminiService()

    fun executeWorkflow(workflowId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val workflow = repository.getWorkflowById(workflowId) ?: return@launch
                if (!workflow.isActive) return@launch

                repository.logInfo("WorkflowExecutor", "بدء تشغيل مسار العمل: '${workflow.name}'")

                val nodes = repository.getNodesForWorkflowSync(workflowId)
                val connections = repository.getConnectionsForWorkflowSync(workflowId)

                if (nodes.isEmpty()) {
                    repository.logFailed("WorkflowExecutor", "مسار العمل فارغ ولا يحتوي على أي عقد.")
                    return@launch
                }

                // Find starting node (no incoming connections, typically a trigger)
                val incomingNodeIds = connections.map { it.toNodeId }.toSet()
                val startNode = nodes.firstOrNull { it.id !in incomingNodeIds } ?: nodes.first()

                executeNodeChain(startNode, nodes, connections)
                repository.logSuccess("WorkflowExecutor", "اكتمل تشغيل مسار العمل '${workflow.name}' بنجاح.")
            } catch (e: Exception) {
                repository.logFailed("WorkflowExecutor", "فشل تشغيل مسار العمل: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun executeNodeChain(
        currentNode: WorkflowNode,
        allNodes: List<WorkflowNode>,
        connections: List<WorkflowConnection>
    ) {
        executeSingleNode(currentNode)

        // Find next connected nodes
        val outgoingConnections = connections.filter { it.fromNodeId == currentNode.id }
        for (conn in outgoingConnections) {
            val nextNode = allNodes.firstOrNull { it.id == conn.toNodeId }
            if (nextNode != null) {
                delay(1500) // Delay between node hops (n8n smart stagger)
                executeNodeChain(nextNode, allNodes, connections)
            }
        }
    }

    private suspend fun executeSingleNode(node: WorkflowNode) {
        val nodeType = node.type.uppercase(Locale.ROOT)
        repository.logInfo("WorkflowExecutor", "تنفيذ العقدة: [${node.label}] من نوع $nodeType")

        try {
            when (nodeType) {
                "TRIGGER_TIME" -> {
                    // Triggers are pre-conditions, just log active state
                    repository.logSuccess(node.label, "تطابق وقت التفعيل: ${node.configValue}")
                }
                "TRIGGER_NOTIFICATION" -> {
                    repository.logSuccess(node.label, "تم التقاط إشعار يطابق: ${node.configValue}")
                }
                "AI_AGENT" -> {
                    val prompt = node.configValue
                    repository.logInfo(node.label, "طلب معالجة الذكاء الاصطناعي: '$prompt'")
                    val response = geminiService.chatWithAgent(
                        chatHistory = emptyList(),
                        currentPrompt = prompt
                    )
                    val reply = response?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (reply != null) {
                        repository.logSuccess(node.label, "رد الوكيل: $reply")
                        // Speak response or log it
                        commandExecutor.executeAction("TTS", reply)
                    } else {
                        throw Exception("لم يتم استلام رد صالح من Gemini.")
                    }
                }
                "DELAY" -> {
                    val ms = node.configValue.toLongOrNull() ?: 2000L
                    repository.logInfo(node.label, "انتظار لمدة $ms مللي ثانية...")
                    delay(ms)
                    repository.logSuccess(node.label, "اكتمل الانتظار.")
                }
                "HTTP_REQUEST" -> {
                    val url = node.configValue
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        throw Exception("عنوان URL غير صالح: $url")
                    }
                    repository.logInfo(node.label, "إرسال طلب HTTP GET إلى $url")
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "Android-AI-Automation-Agent")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string() ?: ""
                            val preview = if (body.length > 150) body.take(150) + "..." else body
                            repository.logSuccess(node.label, "نجاح الطلب (كود ${response.code}): $preview")
                        } else {
                            throw Exception("فشل كود الحالة: ${response.code}")
                        }
                    }
                }
                "ACTION_TTS" -> {
                    commandExecutor.executeAction("TTS", node.configValue)
                }
                "ACTION_OPEN_APP" -> {
                    commandExecutor.executeAction("OPEN_APP", node.configValue)
                }
                "ACTION_SMS" -> {
                    commandExecutor.executeAction("SEND_SMS", node.configValue)
                }
                else -> {
                    // Try general command executor fallback
                    commandExecutor.executeAction(nodeType, node.configValue)
                }
            }
        } catch (e: Exception) {
            repository.logFailed(node.label, "خطأ أثناء تنفيذ العقدة: ${e.localizedMessage}")
            throw e
        }
    }
}
