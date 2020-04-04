package com.github.niuhf0452.exile.web

import com.github.niuhf0452.exile.common.IntegrationApi
import com.github.niuhf0452.exile.common.PublicApi
import kotlinx.coroutines.ThreadContextElement
import org.slf4j.MDC
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

@PublicApi
class WorkerContext : AbstractCoroutineContextElement(Key), ThreadContextElement<String?> {
    private val stateMap = ConcurrentHashMap<String, Any>()

    @Volatile
    private var contextId: String = ""

    override fun updateThreadContext(context: CoroutineContext): String? {
        val oldState = MDC.get(mdcKey)
        MDC.put(mdcKey, contextId)
        return oldState
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: String?) {
        if (oldState == null) {
            MDC.remove(mdcKey)
        } else {
            MDC.put(mdcKey, oldState)
        }
    }

    fun setId(value: String) {
        contextId = value
        MDC.put(mdcKey, value)
    }

    fun getId(): String {
        return contextId
    }

    fun <T> get(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return stateMap[key] as? T
    }

    fun set(key: String, value: Any?) {
        if (value == null) {
            stateMap.remove(key)
        } else {
            stateMap[key] = value
        }
    }

    companion object Key : CoroutineContext.Key<WorkerContext> {
        private const val mdcKey = "contextId"
        private val idSeed = AtomicLong(System.currentTimeMillis())

        @IntegrationApi
        fun nextId(): String {
            return idSeed.incrementAndGet().toString()
        }
    }
}