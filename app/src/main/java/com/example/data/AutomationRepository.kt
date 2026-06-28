package com.example.data

import kotlinx.coroutines.flow.Flow

class AutomationRepository(val db: AppDatabase) {
    val allRules: Flow<List<AutomationRule>> = db.automationRuleDao().getAllRules()
    val allLogs: Flow<List<LogEntry>> = db.logEntryDao().getAllLogs()

    // Workflows API
    val allWorkflows: Flow<List<Workflow>> = db.workflowDao().getAllWorkflows()

    suspend fun getActiveWorkflowsSync(): List<Workflow> {
        return db.workflowDao().getActiveWorkflowsSync()
    }

    fun getNodesForWorkflow(workflowId: Int): Flow<List<WorkflowNode>> {
        return db.workflowDao().getNodesForWorkflow(workflowId)
    }

    suspend fun getNodesForWorkflowSync(workflowId: Int): List<WorkflowNode> {
        return db.workflowDao().getNodesForWorkflowSync(workflowId)
    }

    fun getConnectionsForWorkflow(workflowId: Int): Flow<List<WorkflowConnection>> {
        return db.workflowDao().getConnectionsForWorkflow(workflowId)
    }

    suspend fun getConnectionsForWorkflowSync(workflowId: Int): List<WorkflowConnection> {
        return db.workflowDao().getConnectionsForWorkflowSync(workflowId)
    }

    suspend fun getWorkflowById(workflowId: Int): Workflow? {
        return db.workflowDao().getWorkflowById(workflowId)
    }

    suspend fun insertWorkflow(workflow: Workflow): Long {
        return db.workflowDao().insertWorkflow(workflow)
    }

    suspend fun insertNode(node: WorkflowNode): Long {
        return db.workflowDao().insertNode(node)
    }

    suspend fun insertConnection(connection: WorkflowConnection): Long {
        return db.workflowDao().insertConnection(connection)
    }

    suspend fun updateWorkflow(workflow: Workflow) {
        db.workflowDao().updateWorkflow(workflow)
    }

    suspend fun updateNodePosition(nodeId: Int, x: Float, y: Float) {
        db.workflowDao().updateNodePosition(nodeId, x, y)
    }

    suspend fun deleteWorkflow(workflow: Workflow) {
        db.workflowDao().deleteNodesForWorkflow(workflow.id)
        db.workflowDao().deleteConnectionsForWorkflow(workflow.id)
        db.workflowDao().deleteWorkflow(workflow)
    }

    suspend fun deleteNode(nodeId: Int) {
        db.workflowDao().deleteConnectionsForNode(nodeId)
        db.workflowDao().deleteNodeById(nodeId)
    }

    suspend fun deleteConnectionsForWorkflow(workflowId: Int) {
        db.workflowDao().deleteConnectionsForWorkflow(workflowId)
    }

    suspend fun getActiveRules(): List<AutomationRule> {
        return db.automationRuleDao().getActiveRules()
    }

    suspend fun insertRule(rule: AutomationRule) {
        db.automationRuleDao().insertRule(rule)
    }

    suspend fun updateRule(rule: AutomationRule) {
        db.automationRuleDao().updateRule(rule)
    }

    suspend fun deleteRule(rule: AutomationRule) {
        db.automationRuleDao().deleteRule(rule)
    }

    suspend fun deleteRuleById(id: Int) {
        db.automationRuleDao().deleteRuleById(id)
    }

    suspend fun insertLog(log: LogEntry) {
        db.logEntryDao().insertLog(log)
    }

    suspend fun logInfo(actionName: String, message: String) {
        db.logEntryDao().insertLog(LogEntry(actionName = actionName, status = "INFO", message = message))
    }

    suspend fun logSuccess(actionName: String, message: String) {
        db.logEntryDao().insertLog(LogEntry(actionName = actionName, status = "SUCCESS", message = message))
    }

    suspend fun logFailed(actionName: String, message: String) {
        db.logEntryDao().insertLog(LogEntry(actionName = actionName, status = "FAILED", message = message))
    }

    suspend fun clearLogs() {
        db.logEntryDao().clearLogs()
    }
}
