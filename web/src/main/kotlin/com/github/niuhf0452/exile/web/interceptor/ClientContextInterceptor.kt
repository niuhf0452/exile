package com.github.niuhf0452.exile.web.interceptor

import com.github.niuhf0452.exile.common.Order
import com.github.niuhf0452.exile.common.Orders
import com.github.niuhf0452.exile.web.*
import kotlin.coroutines.coroutineContext

@Order(Orders.DEFAULT - 1000)
class ClientContextInterceptor(
        private val headerName: String = CommonHeaders.XContextId
) : WebInterceptor {
    override suspend fun onRequest(request: WebRequest<ByteArray>, handler: WebInterceptor.RequestHandler): WebResponse<ByteArray> {
        val contextId = coroutineContext[WorkerContext]?.getId()
        if (contextId != null) {
            request.headers.set(headerName, listOf(contextId))
        }
        return handler.onRequest(request)
    }
}