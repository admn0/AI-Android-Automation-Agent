package com.example.executor

import android.content.Context
import android.util.Log
import com.example.data.AutomationRepository
import com.example.data.AutomationRule
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class AutomationEngine(
    private val context: Context,
    private val repository: AutomationRepository
) {
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val executor = CommandExecutor(context, repository)
    private val workflowExecutor = WorkflowExecutor(context, repository)
    private var engineJob: Job? = null

    companion object {
        @Volatile
        var isSafeModeEnabled: Boolean = false
    }

    fun startEngine() {
        if (engineJob != null) return
        
        engineJob = engineScope.launch {
            repository.logInfo("AutomationEngine", "بدء عمل محرك الأتمتة المجدولة والمسارات المرئية.")
            while (isActive) {
                if (!isSafeModeEnabled) {
                    evaluateTimeTriggers()
                    evaluateWorkflowTimeTriggers()
                } else {
                    Log.d("AutomationEngine", "Safe Mode active. Skipping triggers.")
                }
                delay(30000) // Check every 30 seconds
            }
        }
    }

    fun stopEngine() {
        engineJob?.cancel()
        engineJob = null
    }

    private suspend fun evaluateTimeTriggers() {
        val rules = repository.getActiveRules()
        val currentTimeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        for (rule in rules) {
            if (rule.triggerType.uppercase(Locale.ROOT) == "TIME") {
                if (rule.triggerValue == currentTimeString) {
                    repository.logInfo(
                        "TimeTrigger",
                        "تطابق مؤقت الوقت المبرمج للشرط: $currentTimeString. تشغيل '${rule.name}'..."
                    )
                    executePipeline(rule)
                }
            }
        }
    }

    private suspend fun evaluateWorkflowTimeTriggers() {
        val currentTimeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        
        withContext(Dispatchers.IO) {
            try {
                val activeWorkflows = repository.getActiveWorkflowsSync()
                for (workflow in activeWorkflows) {
                    val nodes = repository.getNodesForWorkflowSync(workflow.id)
                    val hasMatchingTimeNode = nodes.any { 
                        it.type.uppercase(Locale.ROOT) == "TRIGGER_TIME" && it.configValue == currentTimeString 
                    }
                    if (hasMatchingTimeNode) {
                        repository.logInfo(
                            "WorkflowTimeTrigger",
                            "تطابق مؤقت الوقت المبرمج في مسار '${workflow.name}' للوقت: $currentTimeString. تشغيل المسار..."
                        )
                        workflowExecutor.executeWorkflow(workflow.id)
                    }
                }
            } catch (e: Exception) {
                Log.e("AutomationEngine", "Error checking workflow triggers: ${e.localizedMessage}")
            }
        }
    }

    suspend fun executePipeline(rule: AutomationRule) {
        if (isSafeModeEnabled) {
            repository.logInfo("AutomationEngine", "الوضع الآمن مفعل! تم إلغاء تشغيل الأتمتة '${rule.name}'.")
            return
        }

        repository.logInfo("AutomationPipeline", "البدء في تشغيل مسار الإجراءات لقاعدة الأتمتة '${rule.name}'")
        
        coroutineScope {
            try {
                val steps = rule.actionValue.split(";")
                for ((index, step) in steps.withIndex()) {
                    if (isSafeModeEnabled) {
                        repository.logInfo("AutomationEngine", "تم تفعيل الوضع الآمن أثناء التشغيل المتسلسل. إيقاف الإجراءات.")
                        return@coroutineScope
                    }

                    repository.logInfo("AutomationPipeline", "تنفيذ الخطوة ${index + 1}/${steps.size}: $step")
                    
                    if (index > 0) {
                        delay(2500) 
                    }

                    executor.executeAction(rule.actionType, step.trim())
                }
                
                repository.logSuccess("AutomationPipeline", "اكتمل تشغيل مسار أتمتة '${rule.name}' بنجاح.")
            } catch (e: Exception) {
                repository.logFailed("AutomationPipeline", "حدث خطأ أثناء تشغيل المسار: ${e.localizedMessage}")
            }
        }
    }
}
