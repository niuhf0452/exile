package com.github.niuhf0452.exile.workflow

import kotlinx.serialization.Serializable

/**
 * A plan of workflow to describe the graph of DFA.
 *
 * @since 1.0
 */
@Serializable
data class WorkflowPlanData(val createTime: Long,
                            val stages: List<Stage>,
                            val transitions: List<Transition>) {
    fun getStage(id: Int): Stage? {
        return stages.find { it.id == id }
    }

    fun getTransitionEvent(from: Int, to: Int): String? {
        return transitions.find { it.from == from && it.to == to }?.event
    }

    /**
     * A stage of workflow, it's also a state of DFA.
     */
    @Serializable
    data class Stage(val id: Int, val title: String, val type: StageType, val action: Action)

    enum class StageType {
        /**
         * Start stage.
         */
        START,

        /**
         * Terminal stage. A workflow may have one or more terminal state.
         */
        TERMINAL,

        /**
         * A user stage. That means the stage will wait for user input.
         */
        USER,

        /**
         * A automatically process stage.
         */
        AUTO
    }

    @Serializable
    data class Action(val name: String, val parameters: Map<String, String>)

    /**
     * A transition from stage to next stage. The transition should take place when the event is received.
     */
    @Serializable
    data class Transition(val from: Int, val to: Int, val event: String)
}