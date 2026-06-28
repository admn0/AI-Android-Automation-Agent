package com.example.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.AutomationRepository
import com.example.data.LogEntry
import com.example.executor.CommandExecutor
import com.example.executor.WorkflowExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MyNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var repository: AutomationRepository
    private lateinit var executor: CommandExecutor
    private lateinit var workflowExecutor: WorkflowExecutor

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(this)
        repository = AutomationRepository(db)
        executor = CommandExecutor(this, repository)
        workflowExecutor = WorkflowExecutor(this, repository)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val sbnNotNull = sbn ?: return
        val packageName = sbnNotNull.packageName
        val extras = sbnNotNull.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        Log.d("NotificationListener", "Notification from $packageName: $title - $text")

        serviceScope.launch {
            // Log raw notification capture in history
            repository.insertLog(
                LogEntry(
                    actionName = "NotificationListener",
                    status = "INFO",
                    message = "إشعار وارد من $packageName | $title: $text"
                )
            )

            // 1. Check simple rules
            val activeRules = repository.getActiveRules()
            for (rule in activeRules) {
                if (rule.triggerType == "NOTIFICATION") {
                    val keyword = rule.triggerValue
                    if (text.contains(keyword, ignoreCase = true) || packageName.contains(keyword, ignoreCase = true)) {
                        repository.logInfo(
                            "NotificationTrigger",
                            "تم رصد إشعار يطابق القاعدة '${rule.name}'. البدء في تنفيذ '${rule.actionType}'"
                        )
                        executor.executeAction(rule.actionType, rule.actionValue)
                    }
                }
            }

            // 2. Check workflow visual nodes
            try {
                val activeWorkflows = repository.getActiveWorkflowsSync()
                for (workflow in activeWorkflows) {
                    val nodes = repository.getNodesForWorkflowSync(workflow.id)
                    val matchingTrigger = nodes.firstOrNull { node ->
                        node.type.uppercase(Locale.ROOT) == "TRIGGER_NOTIFICATION" && 
                        (text.contains(node.configValue, ignoreCase = true) || packageName.contains(node.configValue, ignoreCase = true) || title.contains(node.configValue, ignoreCase = true))
                    }
                    if (matchingTrigger != null) {
                        repository.logInfo(
                            "WorkflowNotificationTrigger",
                            "تم التقاط إشعار يطابق عقدة التفعيل [${matchingTrigger.label}] في مسار '${workflow.name}'."
                        )
                        workflowExecutor.executeWorkflow(workflow.id)
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationListener", "Error triggering workflow nodes: ${e.localizedMessage}")
            }
        }
    }
}
