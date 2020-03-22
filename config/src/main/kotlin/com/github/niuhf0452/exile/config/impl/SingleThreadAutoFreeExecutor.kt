package com.github.niuhf0452.exile.config.impl

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * An executor run tasks in a single thread, and auto release the thread after run.
 */
class SingleThreadAutoFreeExecutor(
        private val threadName: String,
        private val isDaemon: Boolean = true
) : Executor {
    private val dirtyCounter = AtomicInteger(0)
    private val taskQueue = ConcurrentLinkedQueue<Runnable>()

    override fun execute(command: Runnable) {
        taskQueue.offer(command)
        if (dirtyCounter.getAndIncrement() == 0) {
            thread(isDaemon = isDaemon, name = threadName) {
                var c = dirtyCounter.get()
                while (c != 0) {
                    handleTasks()
                    c = dirtyCounter.addAndGet(-c)
                }
            }
        }
    }

    private fun handleTasks() {
        while (true) {
            val task = taskQueue.poll()
                    ?: break
            task.run()
        }
    }
}