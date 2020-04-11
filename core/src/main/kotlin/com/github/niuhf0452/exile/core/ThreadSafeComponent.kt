package com.github.niuhf0452.exile.core

abstract class ThreadSafeComponent : Component {
    private val stateLock = Object()
    private var state: RunState = RunState.NotStart

    protected abstract fun safeStart()
    protected abstract fun safeStop()

    final override fun start() {
        synchronized(stateLock) {
            when (state) {
                RunState.NotStart -> setState(RunState.Starting(Thread.currentThread()))
                is RunState.Starting, RunState.HalfStarted, RunState.Stopping -> Unit
                RunState.Stopped -> throw IllegalStateException("Component is stopped.")
            }
        }
        try {
            safeStart()
            synchronized(stateLock) {
                if (Thread.currentThread().isInterrupted) {
                    throw InterruptedException()
                }
                setState(RunState.Running)
            }
        } catch (ex: InterruptedException) {
            synchronized(stateLock) {
                setState(RunState.HalfStarted)
            }
            throw Component.StopException
        }
    }

    final override fun stop() {
        synchronized(stateLock) {
            when (val s = state) {
                is RunState.Starting -> {
                    s.thread.interrupt()
                    waitFor(RunState.HalfStarted)
                    setState(RunState.Stopping)
                }
                RunState.HalfStarted -> {
                    setState(RunState.Stopped)
                    return
                }
                RunState.Running -> setState(RunState.Stopping)
                RunState.NotStart -> {
                    setState(RunState.Stopped)
                    return
                }
                RunState.Stopping -> {
                    waitFor(RunState.Stopped)
                    return
                }
                RunState.Stopped -> return
            }
        }
        safeStop()
        synchronized(stateLock) {
            setState(RunState.Stopped)
        }
    }

    fun awaitTerminal() {
        synchronized(stateLock) {
            waitFor(RunState.Stopped)
        }
    }

    private fun setState(newState: RunState) {
        state = newState
        stateLock.notifyAll()
    }

    private fun waitFor(waitState: RunState) {
        while (state != waitState) {
            stateLock.wait()
        }
    }

    private sealed class RunState {
        object NotStart : RunState()

        class Starting(val thread: Thread) : RunState()

        object HalfStarted : RunState()

        object Running : RunState()

        object Stopping : RunState()

        object Stopped : RunState()
    }
}