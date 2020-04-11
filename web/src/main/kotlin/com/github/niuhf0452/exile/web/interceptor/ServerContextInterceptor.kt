package com.github.niuhf0452.exile.web.interceptor

import com.github.niuhf0452.exile.common.Order
import com.github.niuhf0452.exile.common.Orders
import com.github.niuhf0452.exile.web.*
import kotlinx.coroutines.withContext

@Order(Orders.DEFAULT - 1000)
class ServerContextInterceptor(
        private val headerName: String = CommonHeaders.XContextId
) : WebInterceptor {
    override suspend fun onRequest(request: WebRequest<ByteArray>, handler: WebInterceptor.RequestHandler): WebResponse<ByteArray> {
        val context = WorkerContext()
        val contextId = request.headers.get(headerName).firstOrNull()
                ?: WorkerContext.nextId()
        context.setId(contextId)
        return withContext(context) {
            handler.onRequest(request)
        }
    }
}