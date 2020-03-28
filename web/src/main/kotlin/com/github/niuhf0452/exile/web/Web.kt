package com.github.niuhf0452.exile.web

import com.github.niuhf0452.exile.web.impl.WebRequestImpl
import com.github.niuhf0452.exile.web.impl.WebResponseImpl
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerialModule
import java.net.URI
import java.time.Duration
import javax.net.ssl.SSLContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

interface WebServer : AutoCloseable, Router {
    val port: Int

    @Serializable
    data class Config(
            val host: String = "localhost",
            val port: Int = 0,
            val contextPath: String = "/",
            val maxRequestLineSize: Int = 1024,
            val maxHeaderSize: Int = 4 * 1024,
            val maxEntitySize: Int = 512 * 1024,
            val keepAlive: Boolean = true
    )

    interface Factory {
        fun startServer(config: Config, coroutineContext: CoroutineContext): WebServer
    }
}

interface Router {
    fun addRoute(method: String, path: String, handler: WebHandler)

    fun removeRoute(method: String, path: String)

    suspend fun onRequest(request: WebRequest<ByteArray>): WebResponse<ByteArray>

    fun setExceptionHandler(handler: WebExceptionHandler)
}

interface WebHandler {
    suspend fun onRequest(context: RequestContext, request: WebRequest<Variant>): WebResponse<Any>
}

interface WebExceptionHandler {
    fun handle(exception: Exception): WebResponse<ByteArray>
}

interface RequestContext {
    val routePath: String
    val pathVariables: Map<String, String>

    fun getPathVar(name: String): String {
        return pathVariables[name]
                ?: throw IllegalStateException("Path variable is missing: $name")
    }
}

interface WebRequest<E> {
    val uri: URI
    val method: String
    val headers: WebHeaders
    val entity: E

    interface Builder {
        fun method(value: String): Builder

        fun setPathParam(name: String, value: String): Builder

        fun addQueryParam(name: String, value: String): Builder

        fun setHeaders(value: Map<String, String>): Builder

        fun addHeader(name: String, value: String): Builder

        fun setHeader(name: String, value: Iterable<String>): Builder

        fun removeHeader(name: String): Builder

        fun <T> entity(value: T): WebRequest<T>

        fun noEntity(): WebRequest<ByteArray> = entity(emptyByteArray)
    }

    companion object {
        fun newBuilder(uri: String): Builder {
            return WebRequestImpl.Builder(uri)
        }
    }
}

interface WebResponse<out E> {
    val statusCode: Int
    val headers: WebHeaders
    val entity: E

    interface Builder {
        fun statusCode(value: Int): Builder

        fun headers(value: WebHeaders): Builder

        fun setHeaders(value: Map<String, String>): Builder

        fun addHeader(name: String, value: String): Builder

        fun setHeader(name: String, value: Iterable<String>): Builder

        fun removeHeader(name: String): Builder

        fun <T> entity(value: T): WebResponse<T>

        fun noEntity(): WebResponse<ByteArray> = entity(emptyByteArray)
    }

    companion object {
        fun newBuilder(): Builder {
            return WebResponseImpl.Builder()
        }
    }
}

interface WebHeaders : Iterable<String> {
    fun get(name: String): Iterable<String>

    object Empty : WebHeaders {
        override fun get(name: String): Iterable<String> {
            return emptyList()
        }

        override fun iterator(): Iterator<String> {
            return emptyList<String>().iterator()
        }
    }
}

interface Variant {
    fun <T : Any> convertTo(cls: KClass<T>): T
}

interface WebEntitySerializer {
    fun acceptConsumes(mediaType: MediaType): Boolean

    fun acceptProduces(mediaType: MediaType): Boolean

    fun serialize(data: Any, mediaType: MediaType): ByteArray

    fun deserialize(data: ByteArray, cls: KClass<*>, mediaType: MediaType): Any

    interface Factory {
        fun createSerializer(module: SerialModule): WebEntitySerializer
    }
}

class WebResponseException(val response: WebResponse<ByteArray>)
    : RuntimeException("${response.statusCode} - ${response.entity}") {
    constructor(statusCode: Int, message: String)
            : this(WebResponse.newBuilder()
            .statusCode(statusCode)
            .addHeader("Content-Type", "text/plain")
            .entity(message.toByteArray()))
}

data class SerialModuleElement(val module: SerialModule)
    : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<SerialModuleElement>
}

interface WebClient {
    suspend fun send(request: WebRequest<ByteArray>): WebResponse<ByteArray>

    interface Builder {
        fun maxKeepAliveConnectionSize(value: Int): Builder
        fun connectTimeout(value: Duration): Builder
        fun requestTimeout(value: Duration): Builder
        fun sslContext(value: SSLContext): Builder

        fun build(): WebClient
    }
}