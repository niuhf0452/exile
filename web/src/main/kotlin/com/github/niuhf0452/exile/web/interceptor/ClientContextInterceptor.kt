package com.github.niuhf0452.exile.web.interceptor

import com.github.niuhf0452.exile.web.*
import kotlin.coroutines.coroutineContext

class ClientContextInterceptor(
        private val headerName: String = CommonHeaders.XContextId
) : WebInterceptor {
    override val order: Int
        get() = -1000

    override suspend fun onRequest(request: WebRequest<ByteArray>, handler: WebInterceptor.RequestHandler): WebResponse<ByteArray> {
        val contextId = coroutineContext[WorkerContext]?.getId()
        if (contextId != null) {
            request.headers.set(headerName, listOf(contextId))
        }
        return handler.onRequest(request)
    }
}