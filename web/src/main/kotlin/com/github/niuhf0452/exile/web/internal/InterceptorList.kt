package com.github.niuhf0452.exile.web.internal

import com.github.niuhf0452.exile.web.WebInterceptor
import com.github.niuhf0452.exile.web.WebRequest
import com.github.niuhf0452.exile.web.WebResponse
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass

class InterceptorList(list: List<WebInterceptor>) {
    private val interceptors = AtomicReference<List<WebInterceptor>>(emptyList())

    init {
        if (list.isNotEmpty()) {
            val arr = ArrayList<WebInterceptor>()
            val set = mutableSetOf<KClass<*>>()
            list.forEach { i ->
                if (set.add(i::class)) {
                    arr.add(i)
                }
            }
            arr.sortWith(Comparator { a, b -> a.order - b.order })
            interceptors.set(arr)
        }
    }

    fun add(interceptor: WebInterceptor) {
        do {
            val old = interceptors.get()
            val arr = ArrayList(old)
            val cls = interceptor::class
            arr.removeIf { cls.isInstance(it) }
            arr.add(interceptor)
            arr.sortWith(Comparator { a, b -> a.order - b.order })
        } while (!interceptors.compareAndSet(old, arr))
    }

    fun remove(cls: KClass<*>) {
        do {
            val old = interceptors.get()
            val arr = old.filter { i -> !cls.isInstance(i) }
        } while (!interceptors.compareAndSet(old, arr))
    }

    suspend fun handleRequest(request: WebRequest<ByteArray>,
                              f: suspend (WebRequest<ByteArray>) -> WebResponse<ByteArray>): WebResponse<ByteArray> {
        val arr = interceptors.get()
        if (arr.isEmpty()) {
            return f(request)
        }
        val caller = Caller(arr, object : WebInterceptor.RequestHandler {
            override suspend fun onRequest(request: WebRequest<ByteArray>): WebResponse<ByteArray> {
                return f(request)
            }
        })
        return caller.onRequest(request)
    }

    private class Caller(
            private val interceptors: List<WebInterceptor>,
            private val handler: WebInterceptor.RequestHandler
    ) : WebInterceptor.RequestHandler {
        override suspend fun onRequest(request: WebRequest<ByteArray>): WebResponse<ByteArray> {
            return getHandler(0).onRequest(request)
        }

        private fun getHandler(index: Int): WebInterceptor.RequestHandler {
            return if (index < interceptors.size) {
                Handler(index)
            } else {
                handler
            }
        }

        private inner class Handler(
                private val index: Int
        ) : WebInterceptor.RequestHandler {
            override suspend fun onRequest(request: WebRequest<ByteArray>): WebResponse<ByteArray> {
                return interceptors[index].onRequest(request, getHandler(index + 1))
            }
        }
    }
}