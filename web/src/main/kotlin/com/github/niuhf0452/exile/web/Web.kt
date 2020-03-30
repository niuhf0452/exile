package com.github.niuhf0452.exile.web

import com.github.niuhf0452.exile.web.internal.WebRequestImpl
import com.github.niuhf0452.exile.web.internal.WebResponseImpl
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerialModule
import java.net.URI
import java.time.Duration
import javax.net.ssl.SSLContext
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
    suspend fun onRequest(context: RequestContext): WebResponse<Any>
}

interface WebExceptionHandler {
    fun handle(exception: Exception): WebResponse<ByteArray>
}

interface RequestContext {
    val routePath: String
    val pathParams: Map<String, String>
    val queryParams: MultiValueMap
    val request: WebRequest<Variant>
}

interface WebRequest<out E> {
    val uri: URI
    val method: String
    val headers: MultiValueMap
    val hasEntity: Boolean
    val entity: E

    fun mapHeaders(f: (MultiValueMap) -> MultiValueMap): WebRequest<E>
    fun <A> mapEntity(f: (E) -> A): WebRequest<A>

    interface Builder<out E> {
        fun method(value: String): Builder<E>

        fun setPathParam(name: String, value: String): Builder<E>

        fun addQueryParam(name: String, value: String): Builder<E>

        fun setHeaders(value: Map<String, String>): Builder<E>

        fun addHeader(name: String, value: String): Builder<E>

        fun setHeader(name: String, value: Iterable<String>): Builder<E>

        fun removeHeader(name: String): Builder<E>

        fun <T> entity(value: T): Builder<T>

        fun noEntity(): Builder<Nothing>

        fun build(): WebRequest<E>
    }

    companion object {
        fun newBuilder(uri: String): Builder<Nothing> {
            @Suppress("UNCHECKED_CAST")
            return WebRequestImpl.Builder(uri) as Builder<Nothing>
        }
    }
}

interface WebResponse<out E> {
    val statusCode: Int
    val headers: MultiValueMap
    val hasEntity: Boolean
    val entity: E

    fun mapHeaders(f: (MultiValueMap) -> MultiValueMap): WebResponse<E>
    fun <A> mapEntity(f: (E) -> A): WebResponse<A>

    interface Builder<E> {
        fun statusCode(value: Int): Builder<E>

        fun headers(value: MultiValueMap): Builder<E>

        fun setHeaders(value: Map<String, String>): Builder<E>

        fun addHeader(name: String, value: String): Builder<E>

        fun setHeader(name: String, value: Iterable<String>): Builder<E>

        fun removeHeader(name: String): Builder<E>

        fun <T> entity(value: T): Builder<T>

        fun noEntity(): Builder<Nothing>

        fun build(): WebResponse<E>
    }

    companion object {
        fun newBuilder(): Builder<Nothing> {
            @Suppress("UNCHECKED_CAST")
            return WebResponseImpl.Builder() as Builder<Nothing>
        }
    }
}

interface MultiValueMap : Iterable<String> {
    val isEmpty: Boolean

    fun get(name: String): Iterable<String>

    object Empty : MultiValueMap {
        override val isEmpty: Boolean
            get() = true

        override fun get(name: String): Iterable<String> {
            return emptyList()
        }

        override fun iterator(): Iterator<String> {
            return emptyList<String>().iterator()
        }
    }
}

interface Variant {
    val isEmpty: Boolean

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