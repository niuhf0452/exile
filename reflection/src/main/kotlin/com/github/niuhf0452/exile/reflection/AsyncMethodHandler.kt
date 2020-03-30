package com.github.niuhf0452.exile.reflection

import kotlinx.coroutines.runBlocking
import kotlin.reflect.KFunction

abstract class AsyncMethodHandler<in A> : ProxyMethodHandler<A> {
    protected abstract val function: KFunction<*>

    protected abstract suspend fun asyncCall(state: A, instance: Any, args: Array<out Any?>?): Any?

    final override fun call(state: A, instance: Any, args: Array<out Any?>?): Any? {
        if (function.isSuspend) {
            args!!
            val args0 = if (args.size == 1) {
                null
            } else {
                java.util.Arrays.copyOf(args, args.size - 1)
            }
            val continuation = args.last()!!
            return invokeSuspend(continuation) {
                asyncCall(state, instance, args0)
            }
        } else {
            return runBlocking {
                asyncCall(state, instance, args)
            }
        }
    }

    private fun invokeSuspend(continuation: Any, f: suspend () -> Any?): Any? {
        @Suppress("UNCHECKED_CAST")
        return (f as Function1<Any, Any>).invoke(continuation)
    }
}