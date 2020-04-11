package com.github.niuhf0452.exile.workflow

interface WorkflowService {
    suspend fun createPlan(data: WorkflowPlanData): WorkflowPlan

    suspend fun getPlan(id: Long): WorkflowPlan

    suspend fun startWorkflow(planId: Long): Workflow

    suspend fun getWorkflow(id: Long): Workflow?
}

interface WorkflowPlan {
    suspend fun activate(version: Int)

    suspend fun getData(version: Int): WorkflowPlanData?

    suspend fun updateDraft(data: WorkflowPlanData)

    suspend fun getDraft(): WorkflowPlanData?

    suspend fun dropDraft()

    suspend fun listVersions(createTime: Long, limit: Int): List<VersionBrief>

    data class VersionBrief(val version: Int, val comment: String, val state: VersionState)

    enum class VersionState {
        DRAFT, ACTIVE, LOCK
    }
}

interface Workflow {
    val planId: Long
    val planVersion: Int
    val currentStage: Int

    suspend fun onEvent(event: Event)

    suspend fun getPlan(): WorkflowPlanData

    suspend fun getHistories(): List<History>

    data class History(val stage: Int, val comment: String)
}

interface Event {
    val name: String
}