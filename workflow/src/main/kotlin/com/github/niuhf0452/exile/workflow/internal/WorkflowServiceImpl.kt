package com.github.niuhf0452.exile.workflow.internal

import com.github.niuhf0452.exile.workflow.Workflow
import com.github.niuhf0452.exile.workflow.WorkflowPlan
import com.github.niuhf0452.exile.workflow.WorkflowPlanData
import com.github.niuhf0452.exile.workflow.WorkflowService

class WorkflowServiceImpl(
        private val workflowPlanRepository: WorkflowPlanRepository,
        private val workflowRepository: WorkflowRepository
) : WorkflowService {
    override suspend fun createPlan(data: WorkflowPlanData): WorkflowPlan {
        val stageIds = mutableSetOf<Int>()
        data.stages.forEach { stage ->
            if (!stageIds.add(stage.id)) {
                throw IllegalArgumentException("")
            }
        }
        data.copy(createTime = System.currentTimeMillis())
        TODO("not implemented")
    }

    private suspend fun checkStage(value: WorkflowPlanData.Stage) {
        value.id
    }

    override suspend fun getPlan(id: Long): WorkflowPlan {
        TODO("not implemented")
    }

    override suspend fun startWorkflow(planId: Long): Workflow {
        TODO("not implemented")
    }

    override suspend fun getWorkflow(id: Long): Workflow? {
        TODO("not implemented")
    }

    class WorkflowPlanImpl : WorkflowPlan {
        override suspend fun activate(version: Int) {
            TODO("not implemented")
        }

        override suspend fun getData(version: Int): WorkflowPlanData? {
            TODO("not implemented")
        }

        override suspend fun updateDraft(data: WorkflowPlanData) {
            TODO("not implemented")
        }

        override suspend fun getDraft(): WorkflowPlanData? {
            TODO("not implemented")
        }

        override suspend fun dropDraft() {
            TODO("not implemented")
        }

        override suspend fun listVersions(createTime: Long, limit: Int): List<WorkflowPlan.VersionBrief> {
            TODO("not implemented")
        }
    }
}