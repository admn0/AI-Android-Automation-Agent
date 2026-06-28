package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.Content
import com.example.ai.GeminiService
import com.example.ai.Part
import com.example.data.*
import com.example.executor.AutomationEngine
import com.example.executor.CommandExecutor
import com.example.executor.WorkflowExecutor
import com.example.services.MyAccessibilityService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AutomationRepository = AutomationRepository(AppDatabase.getDatabase(application))
    private val geminiService = GeminiService()
    private val executor: CommandExecutor = CommandExecutor(application, repository)
    private val workflowExecutor = WorkflowExecutor(application, repository)

    val allRules: StateFlow<List<AutomationRule>>
    val allLogs: StateFlow<List<LogEntry>>

    // Workflows State Management
    val allWorkflows: StateFlow<List<Workflow>>
    
    private val _selectedWorkflow = MutableStateFlow<Workflow?>(null)
    val selectedWorkflow: StateFlow<Workflow?> = _selectedWorkflow.asStateFlow()

    private val _activeNodes = MutableStateFlow<List<WorkflowNode>>(emptyList())
    val activeNodes: StateFlow<List<WorkflowNode>> = _activeNodes.asStateFlow()

    private val _activeConnections = MutableStateFlow<List<WorkflowConnection>>(emptyList())
    val activeConnections: StateFlow<List<WorkflowConnection>> = _activeConnections.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList())
    val chatMessages: StateFlow<List<Pair<String, Boolean>>> = _chatMessages.asStateFlow()

    private val _isSafeMode = MutableStateFlow(AutomationEngine.isSafeModeEnabled)
    val isSafeMode: StateFlow<Boolean> = _isSafeMode.asStateFlow()

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    private val _isOverlayEnabled = MutableStateFlow(false)
    val isOverlayEnabled: StateFlow<Boolean> = _isOverlayEnabled.asStateFlow()

    init {
        allRules = repository.allRules.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allLogs = repository.allLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allWorkflows = repository.allWorkflows.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        checkPermissions()
        initializeDefaultWorkflowsIfNeeded()
        observeSelectedWorkflowChanges()
    }

    private fun observeSelectedWorkflowChanges() {
        viewModelScope.launch {
            _selectedWorkflow.collectLatest { workflow ->
                if (workflow != null) {
                    repository.getNodesForWorkflow(workflow.id).collectLatest { nodes ->
                        _activeNodes.value = nodes
                    }
                } else {
                    _activeNodes.value = emptyList()
                }
            }
        }
        viewModelScope.launch {
            _selectedWorkflow.collectLatest { workflow ->
                if (workflow != null) {
                    repository.getConnectionsForWorkflow(workflow.id).collectLatest { conns ->
                        _activeConnections.value = conns
                    }
                } else {
                    _activeConnections.value = emptyList()
                }
            }
        }
    }

    private fun initializeDefaultWorkflowsIfNeeded() {
        viewModelScope.launch {
            repository.allWorkflows.first().let { workflows ->
                if (workflows.isEmpty()) {
                    // Create Good Morning Template
                    val morningWfId = repository.insertWorkflow(
                        Workflow(
                            name = "مساعد الصباح الذكي",
                            description = "عند الساعة الثامنة والنصف ينطق ترحيباً بالكامل ثم يفتح الهاتف.",
                            isActive = true
                        )
                    )
                    val n1 = repository.insertNode(
                        WorkflowNode(
                            workflowId = morningWfId.toInt(),
                            type = "TRIGGER_TIME",
                            label = "منبه 08:30",
                            x = 100f,
                            y = 200f,
                            configValue = "08:30"
                        )
                    )
                    val n2 = repository.insertNode(
                        WorkflowNode(
                            workflowId = morningWfId.toInt(),
                            type = "DELAY",
                            label = "انتظار ثانيتين",
                            x = 350f,
                            y = 200f,
                            configValue = "2000"
                        )
                    )
                    val n3 = repository.insertNode(
                        WorkflowNode(
                            workflowId = morningWfId.toInt(),
                            type = "ACTION_TTS",
                            label = "نطق صوتي",
                            x = 600f,
                            y = 200f,
                            configValue = "صباح الخير يا صديقي، أتمنى لك يوماً رائعاً ملئ بالإنتاج والنشاط."
                        )
                    )
                    repository.insertConnection(WorkflowConnection(workflowId = morningWfId.toInt(), fromNodeId = n1.toInt(), toNodeId = n2.toInt()))
                    repository.insertConnection(WorkflowConnection(workflowId = morningWfId.toInt(), fromNodeId = n2.toInt(), toNodeId = n3.toInt()))

                    // Create Notification Smart Checker Template
                    val notifWfId = repository.insertWorkflow(
                        Workflow(
                            name = "محلل الإشعارات الذكي",
                            description = "عند استلام إشعار يحوي 'فاتورة'، يحلله الذكاء الاصطناعي ويصدر تتبيه مباشر.",
                            isActive = true
                        )
                    )
                    val n4 = repository.insertNode(
                        WorkflowNode(
                            workflowId = notifWfId.toInt(),
                            type = "TRIGGER_NOTIFICATION",
                            label = "إشعار يحوي 'فاتورة'",
                            x = 100f,
                            y = 350f,
                            configValue = "فاتورة"
                        )
                    )
                    val n5 = repository.insertNode(
                        WorkflowNode(
                            workflowId = notifWfId.toInt(),
                            type = "AI_AGENT",
                            label = "تحليل بالذكاء الاصطناعي",
                            x = 380f,
                            y = 350f,
                            configValue = "قم بصياغة ملخص فوري بلهجة ودية عن استلام فاتورة جديدة تبلغ المستخدم بضرورة سدادها في الوقت المحدد"
                        )
                    )
                    repository.insertConnection(WorkflowConnection(workflowId = notifWfId.toInt(), fromNodeId = n4.toInt(), toNodeId = n5.toInt()))

                    // Set morning workflow as default selected
                    val created = repository.allWorkflows.first()
                    if (created.isNotEmpty()) {
                        _selectedWorkflow.value = created.first()
                    }
                } else if (_selectedWorkflow.value == null) {
                    _selectedWorkflow.value = workflows.first()
                }
            }
        }
    }

    fun selectWorkflow(workflow: Workflow) {
        _selectedWorkflow.value = workflow
    }

    fun addNewWorkflow(name: String, description: String) {
        viewModelScope.launch {
            val workflowId = repository.insertWorkflow(Workflow(name = name, description = description, isActive = true))
            val created = repository.getWorkflowById(workflowId.toInt())
            if (created != null) {
                _selectedWorkflow.value = created
            }
        }
    }

    fun deleteActiveWorkflow() {
        val active = _selectedWorkflow.value ?: return
        viewModelScope.launch {
            repository.deleteWorkflow(active)
            _selectedWorkflow.value = null
            repository.allWorkflows.first().let { list ->
                if (list.isNotEmpty()) {
                    _selectedWorkflow.value = list.first()
                }
            }
        }
    }

    fun executeActiveWorkflowDirectly() {
        val active = _selectedWorkflow.value ?: return
        workflowExecutor.executeWorkflow(active.id)
    }

    fun updateNodePosition(nodeId: Int, x: Float, y: Float) {
        viewModelScope.launch {
            repository.updateNodePosition(nodeId, x, y)
        }
    }

    fun addNodeToActiveWorkflow(type: String, label: String, configValue: String) {
        val active = _selectedWorkflow.value ?: return
        viewModelScope.launch {
            repository.insertNode(
                WorkflowNode(
                    workflowId = active.id,
                    type = type,
                    label = label,
                    x = 150f,
                    y = 200f,
                    configValue = configValue
                )
            )
        }
    }

    fun deleteNodeFromActiveWorkflow(nodeId: Int) {
        viewModelScope.launch {
            repository.deleteNode(nodeId)
        }
    }

    fun connectNodesInActiveWorkflow(fromNodeId: Int, toNodeId: Int) {
        val active = _selectedWorkflow.value ?: return
        if (fromNodeId == toNodeId) return
        viewModelScope.launch {
            repository.insertConnection(
                WorkflowConnection(
                    workflowId = active.id,
                    fromNodeId = fromNodeId,
                    toNodeId = toNodeId
                )
            )
        }
    }

    fun deleteConnectionsForActiveWorkflow() {
        val active = _selectedWorkflow.value ?: return
        viewModelScope.launch {
            repository.deleteConnectionsForWorkflow(active.id)
        }
    }

    fun checkPermissions() {
        val context = getApplication<Application>()
        _isAccessibilityEnabled.value = MyAccessibilityService.instance != null
        _isOverlayEnabled.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun toggleSafeMode() {
        val nextVal = !_isSafeMode.value
        AutomationEngine.isSafeModeEnabled = nextVal
        _isSafeMode.value = nextVal
        viewModelScope.launch {
            repository.logInfo(
                "SafeMode",
                if (nextVal) "تم تفعيل الوضع الآمن وتعطيل كافة الأتمتة المبرمجة لحماية الهاتف." else "تم إيقاف الوضع الآمن. الأتمتة مستعدة للعمل."
            )
        }
    }

    fun addNewRule(
        name: String,
        triggerType: String,
        triggerValue: String,
        actionType: String,
        actionValue: String,
        description: String
    ) {
        viewModelScope.launch {
            val newRule = AutomationRule(
                name = name,
                triggerType = triggerType,
                triggerValue = triggerValue,
                actionType = actionType,
                actionValue = actionValue,
                description = description
            )
            repository.insertRule(newRule)
            repository.logInfo("RulesManager", "تمت إضافة قاعدة أتمتة جديدة بنجاح: '$name'")
        }
    }

    fun deleteRule(rule: AutomationRule) {
        viewModelScope.launch {
            repository.deleteRule(rule)
            repository.logInfo("RulesManager", "تم حذف قاعدة الأتمتة: '${rule.name}'")
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun executeRuleDirectly(rule: AutomationRule) {
        viewModelScope.launch {
            repository.logInfo("ManualTrigger", "تشغيل قاعدة الأتمتة يدوياً: '${rule.name}'")
            val steps = rule.actionValue.split(";")
            for (step in steps) {
                executor.executeAction(rule.actionType, step.trim())
            }
        }
    }

    // --- Direct Gemini Chat Interface ---

    fun sendChatCommand(command: String) {
        if (command.isBlank()) return
        
        val currentList = _chatMessages.value.toMutableList()
        currentList.add(command to true)
        _chatMessages.value = currentList

        viewModelScope.launch {
            repository.logInfo("GeminiAgent", "إجراء دردشة مع الوكيل: '$command'")
            
            val history = currentList.map {
                Content(role = if (it.second) "user" else "model", parts = listOf(Part(text = it.first)))
            }

            val screenText = MyAccessibilityService.instance?.getScreenContentText()
            val response = geminiService.chatWithAgent(
                chatHistory = history,
                currentPrompt = command,
                screenContent = screenText
            )

            val candidate = response?.candidates?.firstOrNull()
            val replyText = candidate?.content?.parts?.firstOrNull()?.text
            val functionCall = candidate?.content?.parts?.firstOrNull()?.functionCall

            val updatedList = _chatMessages.value.toMutableList()
            if (functionCall != null) {
                val args = functionCall.args ?: emptyMap()
                
                if (functionCall.name == "create_automation_workflow") {
                    val wName = args["name"] ?: "مسار أتمتة تلقائي"
                    val wDesc = args["description"] ?: "تم إنشاؤه بواسطة الذكاء الاصطناعي"
                    val nodesCsv = args["nodes_csv"] ?: ""
                    val connectionsCsv = args["connections_csv"] ?: ""
                    
                    try {
                        val workflowId = repository.insertWorkflow(
                            Workflow(name = wName, description = wDesc, isActive = true)
                        )
                        
                        val nodeLines = nodesCsv.split("\n")
                        val insertedNodeIds = mutableListOf<Long>()
                        
                        for (line in nodeLines) {
                            if (line.isBlank() || !line.contains("|")) continue
                            val parts = line.split("|")
                            if (parts.size >= 5) {
                                val type = parts[0].trim()
                                val label = parts[1].trim()
                                val x = parts[2].trim().toFloatOrNull() ?: 150f
                                val y = parts[3].trim().toFloatOrNull() ?: 200f
                                val configVal = parts[4].trim()
                                
                                val nodeLongId = repository.insertNode(
                                    WorkflowNode(
                                        workflowId = workflowId.toInt(),
                                        type = type,
                                        label = label,
                                        x = x,
                                        y = y,
                                        configValue = configVal
                                    )
                                )
                                insertedNodeIds.add(nodeLongId)
                            }
                        }
                        
                        val connLines = connectionsCsv.split("\n")
                        for (line in connLines) {
                            if (line.isBlank() || !line.contains("|")) continue
                            val parts = line.split("|")
                            if (parts.size == 2) {
                                val fromIdx = parts[0].trim().toIntOrNull() ?: continue
                                val toIdx = parts[1].trim().toIntOrNull() ?: continue
                                
                                if (fromIdx in insertedNodeIds.indices && toIdx in insertedNodeIds.indices) {
                                    val fromNodeId = insertedNodeIds[fromIdx].toInt()
                                    val toNodeId = insertedNodeIds[toIdx].toInt()
                                    
                                    repository.insertConnection(
                                        WorkflowConnection(
                                            workflowId = workflowId.toInt(),
                                            fromNodeId = fromNodeId,
                                            toNodeId = toNodeId
                                        )
                                    )
                                }
                            }
                        }
                        
                        val createdWf = repository.getWorkflowById(workflowId.toInt())
                        if (createdWf != null) {
                            _selectedWorkflow.value = createdWf
                        }
                        
                        updatedList.add("لقد قمت بإنشاء مسار العمل البصري الجديد بنجاح باسم: '$wName'!\nالوصف: $wDesc\nيمكنك الآن استعراضه وتعديله والتحكم به في شاشة الأتمتة البصرية." to false)
                        _chatMessages.value = updatedList
                        repository.logSuccess("GeminiAgent", "تم بنجاح بناء مسار الأتمتة البصري المتكامل: '$wName'")
                    } catch (e: Exception) {
                        updatedList.add("حدث خطأ أثناء محاولة بناء مسار العمل: ${e.localizedMessage}" to false)
                        _chatMessages.value = updatedList
                    }
                } else {
                    updatedList.add("جاري تشغيل الإجراء: ${functionCall.name}..." to false)
                    _chatMessages.value = updatedList
                    
                    val argVal = args.values.firstOrNull() ?: ""
                    executor.executeAction(functionCall.name, argVal)
                }
            } else if (replyText != null) {
                updatedList.add(replyText to false)
                _chatMessages.value = updatedList
            } else {
                updatedList.add("عذراً، لم أستطع فهم الأمر أو تعذر الاتصال بمزود الذكاء الاصطناعي." to false)
                _chatMessages.value = updatedList
            }
        }
    }
}
