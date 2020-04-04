package com.github.niuhf0452.exile.web.interceptor

import com.github.niuhf0452.exile.web.*
import kotlinx.coroutines.withContext

class ServerContextInterceptor(
        private val headerName: String = CommonHeaders.XContextId
) : WebInterceptor {
    override val order: Int
        get() = -1000

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