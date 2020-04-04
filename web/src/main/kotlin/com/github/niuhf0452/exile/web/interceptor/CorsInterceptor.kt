package com.github.niuhf0452.exile.web.interceptor

import com.github.niuhf0452.exile.common.IntegrationApi
import com.github.niuhf0452.exile.web.CommonHeaders
import com.github.niuhf0452.exile.web.WebInterceptor
import com.github.niuhf0452.exile.web.WebRequest
import com.github.niuhf0452.exile.web.WebResponse
import java.time.Duration

@IntegrationApi
class CorsInterceptor(
        private val allowedOrigins: Set<String> = emptySet(),
        private val allowedMethods: Set<String> = setOf("*"),
        private val allowedHeaders: Set<String> = setOf("*"),
        private val allowCredentials: Boolean = true,
        private val maxAge: Duration = Duration.ofSeconds(60 * 60),
        private val exposedHeaders: List<String> = listOf("*")
) : WebInterceptor {
    override val order: Int
        get() = -400

    override suspend fun onRequest(request: WebRequest<ByteArray>, handler: WebInterceptor.RequestHandler): WebResponse<ByteArray> {
        val origin = request.headers.get(CommonHeaders.Origin).firstOrNull()
                ?: return handler.onRequest(request)
        if (request.method == "OPTION") {
            return handlePreflight(request)
        }
        if ("*" !in allowedOrigins && origin !in allowedOrigins) {
            return WebResponse.newBuilder().statusCode(403).build()
        }
        val response = handler.onRequest(request)
        if (allowCredentials) {
            response.headers.add(CommonHeaders.AccessControlAllowCredentials, "true")
        }
        if (exposedHeaders.isNotEmpty()) {
            response.headers.add(CommonHeaders.AccessControlExposeHeaders, exposedHeaders.joinToString(","))
        }
        return response
    }

    private fun handlePreflight(request: WebRequest<ByteArray>): WebResponse<ByteArray> {
        val methods = request.headers.get(CommonHeaders.AccessControlRequestMethod).toList()
        val headers = request.headers.get(CommonHeaders.AccessControlRequestHeaders).toList()
        val statusCode = when {
            "*" !in allowedMethods && !allowedMethods.containsAll(methods) -> 403
            "*" !in allowedHeaders && !allowedHeaders.containsAll(headers) -> 403
            else -> 200
        }
        val response = WebResponse.newBuilder().statusCode(statusCode).build()
        if (allowedMethods.isNotEmpty()) {
            response.headers.add(CommonHeaders.AccessControlAllowMethods, allowedMethods.joinToString(","))
        }
        if (allowedHeaders.isNotEmpty()) {
            response.headers.add(CommonHeaders.AccessControlAllowHeaders, allowedHeaders.joinToString(","))
        }
        if (!maxAge.isZero) {
            response.headers.add(CommonHeaders.AccessControlMaxAge, maxAge.seconds.toString())
        }
        if (allowCredentials) {
            response.headers.add(CommonHeaders.AccessControlAllowCredentials, "true")
        }
        response.headers.add(CommonHeaders.AccessControlAllowOrigin, allowedOrigins.joinToString(","))
        return response
    }
}