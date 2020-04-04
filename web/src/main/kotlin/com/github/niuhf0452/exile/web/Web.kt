package com.github.niuhf0452.exile.web

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerialModule
import java.time.Duration
import javax.net.ssl.SSLContext
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

/**
 * A web server.
 *
 * @since 1.0
 */
interface WebServer : AutoCloseable {
    /**
     * Get the port the web server is listening on.
     */
    val port: Int

    /**
     * Get the router. Even in runtime, the routes can be modified.
     */
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

    /**
     * A factory to start web server.
     */
    interface Factory {
        /**
         * Start a web server.
         *
         * @param config The configuration of web server. A factory should respect to this parameter.
         * @param coroutineContext A coroutine context to use in the coroutine thread which handles requests.
         *                         The context should always ships a executor.
         * @param router A router to route request.
         *               If it's null, the web server will use a empty router.
         *               However, routes can be changed after the web server created.
         */
        fun startServer(config: Config, coroutineContext: CoroutineContext, router: Router? = null): WebServer
    }
}

/**
 * A router mapping routes to handlers.
 *
 * Note that the [Router] should be thread-safe, and all of its methods should be able to call
 * at any time even during [WebServer] is serving. The router should promise the single change of routes
 * is atomic. That means if a request is during handling, at the same time, the route has been changed,
 * then the request will always be handled successfully with the handler when the handling starts.
 * Only the following requests which come after the route change will respect to the changes.
 *
 * @since 1.0
 */
interface Router {
    /**
     * Add a route and map to handler.
     *
     * A route is matched by HTTP verbs (method) and URL path.
     *
     * Note that placeholders can be used in the path pattern to capture named variables.
     *
     * For example,
     *
     * ```
     * /user/:userId/profile/:profileId
     * ```
     *
     * The placeholder `:userId` will match any invalid character in the segment of path,
     * then put the matched value into the variable userId.
     * A handler can retrieve the variable from [RequestContext.pathParams].
     *
     * For a requested path:
     *
     * ```
     * /user/1/profile/2
     * ```
     *
     * It matches the above pattern, and the userId is 1 and profileId is 2.
     *
     * However, the matching is limited in a single segment. For example, the following path doesn't
     * match the pattern, because the `/1/2` are two segment, but the pattern `:userId` can only match
     * in one segment, so the pattern doesn't match the path.
     *
     * ```
     * /user/1/2/profile/2
     * ```
     *
     * @param method The expected HTTP verb.
     * @param path The expected URL path pattern.
     * @param handler The handler which would handle the request match the route.
     */
    fun addRoute(method: String, path: String, handler: WebHandler)

    /**
     * Remove a route. Do nothing if not route matches the method and path.
     *
     * @param method The HTTP verb of route.
     * @param path The URL path pattern of route.
     */
    fun removeRoute(method: String, path: String)

    /**
     * Handle a request. If no route matches the request, then a response with 404 is returned.
     *
     * @param request HTTP request to handle.
     * @return The HTTP response.
     */
    suspend fun onRequest(request: WebRequest<ByteArray>): WebResponse<ByteArray>

    /**
     * Set an exception handler. The exception handler is called to handle any exceptions thrown
     * from handler and router internally.
     *
     * @param handler An exception handler.
     */
    fun setExceptionHandler(handler: WebExceptionHandler)

    /**
     * Add an interceptor.
     *
     * @param interceptor The interceptor to add.
     */
    fun addInterceptor(interceptor: WebInterceptor)

    /**
     * Remove an interceptor.
     *
     * @param cls The class of interceptor to remove.
     */
    fun removeInterceptor(cls: KClass<*>)
}

/**
 * A handler to handle HTTP request.
 *
 * @since 1.0
 */
interface WebHandler {
    suspend fun onRequest(context: RequestContext): WebResponse<Any>
}

/**
 * A handler to handle exception thrown during HTTP request handling.
 *
 * @since 1.0
 */
interface WebExceptionHandler {
    fun handle(exception: Throwable): WebResponse<ByteArray>
}

/**
 * A context keeps variables captured from HTTP request.
 *
 * @since 1.0
 */
interface RequestContext {
    val routePath: String
    val pathParams: Map<String, String>
    val queryParams: MultiValueMap
    val request: WebRequest<Variant>
}

/**
 * A variant object can be converted to other types.
 *
 * @since 1.0
 */
interface Variant {
    fun <T : Any> convertTo(cls: KClass<T>): T
}

/**
 * A serializer to serialize/deserialize HTTP entity with respect to media type.
 *
 * @since 1.0
 */
interface WebEntitySerializer {
    val mediaTypes: List<MediaType>

    fun serialize(data: Any, mediaType: MediaType): ByteArray

    fun deserialize(data: ByteArray, cls: KClass<*>, mediaType: MediaType): Any

    /**
     * A factory to create serializer.
     * This type is used with JDK ServiceLoader. So please always provides a non-arg constructor.
     */
    interface Factory {
        fun createSerializer(module: SerialModule): WebEntitySerializer
    }
}

/**
 * This exception type will be handled by [Router] internally.
 * Generally the statusCode will be responded as HTTP response statusCode, and
 * the description will be responded as HTTP response entity.
 * But the actual HTTP response is created by [WebExceptionHandler], so how the response
 * looks like is decided by the exception handler.
 *
 * @since 1.0
 */
class FailureResponseException(val statusCode: Int, val description: String)
    : RuntimeException("$statusCode - $description")

/**
 * A HTTP client.
 *
 * @since 1.0
 */
interface WebClient {
    suspend fun send(request: WebRequest<Any>): WebResponse<Variant>

    /**
     * Add an interceptor.
     *
     * @param interceptor The interceptor to add.
     */
    fun addInterceptor(interceptor: WebInterceptor)

    /**
     * Remove an interceptor.
     *
     * @param cls The class of interceptor to remove.
     */
    fun removeInterceptor(cls: KClass<*>)

    interface Builder {
        fun maxKeepAliveConnectionSize(value: Int): Builder
        fun connectTimeout(value: Duration): Builder
        fun requestTimeout(value: Duration): Builder
        fun sslContext(value: SSLContext): Builder
        fun addInterceptor(interceptor: WebInterceptor) : Builder

        fun build(): WebClient
    }
}

/**
 * A HTTP request/response interceptor.
 *
 * @since 1.0
 */
interface WebInterceptor {
    val order: Int

    suspend fun onRequest(request: WebRequest<ByteArray>, handler: RequestHandler): WebResponse<ByteArray>

    interface RequestHandler {
        suspend fun onRequest(request: WebRequest<ByteArray>): WebResponse<ByteArray>
    }
}