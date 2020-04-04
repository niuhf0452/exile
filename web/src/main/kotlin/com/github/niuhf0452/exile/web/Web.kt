package com.github.niuhf0452.exile.web

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerialModule
import java.time.Duration
import javax.net.ssl.SSLContext
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

interface WebServer : AutoCloseable {
    val port: Int

    val router: Router

    @Serializable
    data class Config(
            val host: String = "localhost",
            val port: Int = 0,
            val contextPath: String = "/",
            val maxRequestLineSize: Int = 1024,
            val maxHeaderSize: Int = 4 * 1024,
            val maxEntitySize: Int = 512 * 1024,
            val keepAlive: Boolean = true,
            val serverHeader: String = "Exile/1"
    )

    interface Factory {
        fun startServer(config: Config, coroutineContext: CoroutineContext, router: Router? = null): WebServer
    }
}

interface Router {
    fun addRoute(method: String, path: String, handler: WebHandler)

    fun removeRoute(method: String, path: String)

    suspend fun onRequest(request: WebRequest<ByteArray>): WebResponse<ByteArray>

    fun setExceptionHandler(handler: WebExceptionHandler)
}

interface WebHandler {
    suspend fun onRequest(context: RequestContext): WebResponse<Any>
}

interface WebExceptionHandler {
    fun handle(exception: Throwable): WebResponse<ByteArray>
}

interface RequestContext {
    val routePath: String
    val pathParams: Map<String, String>
    val queryParams: MultiValueMap
    val request: WebRequest<Variant>
}

interface Variant {
    fun <T : Any> convertTo(cls: KClass<T>): T
}

interface WebEntitySerializer {
    val mediaTypes: List<MediaType>

    fun serialize(data: Any, mediaType: MediaType): ByteArray

    fun deserialize(data: ByteArray, cls: KClass<*>, mediaType: MediaType): Any

    interface Factory {
        fun createSerializer(module: SerialModule): WebEntitySerializer
    }
}

class FailureResponseException(val statusCode: Int, val description: String)
    : RuntimeException("$statusCode - $description")

interface WebClient {
    suspend fun send(request: WebRequest<Any>): WebResponse<Variant>

    interface Builder {
        fun maxKeepAliveConnectionSize(value: Int): Builder
        fun connectTimeout(value: Duration): Builder
        fun requestTimeout(value: Duration): Builder
        fun sslContext(value: SSLContext): Builder

        fun build(): WebClient
    }
}