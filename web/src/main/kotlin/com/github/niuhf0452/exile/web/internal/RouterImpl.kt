package com.github.niuhf0452.exile.web.internal

import com.github.niuhf0452.exile.common.URLHelper
import com.github.niuhf0452.exile.web.*
import com.github.niuhf0452.exile.web.serialization.EntitySerializers
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class RouterImpl(
        private val config: WebServer.Config
) : Router {
    private val log = LoggerFactory.getLogger(Router::class.java)
    private val routes = ConcurrentHashMap<RouteKey, Route>()
    private var exceptionHandler: WebExceptionHandler = DefaultExceptionHandler()
    private val interceptors = InterceptorList(emptyList())

    override fun addRoute(method: String, path: String, handler: WebHandler) {
        val p = if (path.startsWith('/')) path else "/$path"
        val key = RouteKey(method, p)
        val route = Route(method, p, segmentPath(p), handler)
        if (routes.putIfAbsent(key, route) != null) {
            throw IllegalArgumentException("The route already exists: $key")
        }
    }

    override fun removeRoute(method: String, path: String) {
        val key = RouteKey(method, path)
        routes.remove(key)
    }

    override suspend fun onRequest(request: WebRequest<ByteArray>): WebResponse<ByteArray> {
        val response = try {
            interceptors.handleRequest(request) { req ->
                try {
                    handleRequest(req)
                } catch (ex: Throwable) {
                    exceptionHandler.handle(ex)
                }
            }
        } catch (ex: Throwable) {
            log.error("Critical error", ex)
            WebResponse.newBuilder().statusCode(500).build()
        }
        // make sure connection header is always set correctly.
        addConnectionHeader(response, request)
        addContentLengthHeader(response)
        addServerHeader(response)
        return response
    }

    override fun setExceptionHandler(handler: WebExceptionHandler) {
        exceptionHandler = when (handler) {
            is DefaultExceptionHandler -> handler
            else -> SafeExceptionHandler(handler)
        }
    }

    override fun addInterceptor(interceptor: WebInterceptor) {
        interceptors.add(interceptor)
    }

    override fun removeInterceptor(cls: KClass<*>) {
        interceptors.remove(cls)
    }

    private suspend fun handleRequest(request: WebRequest<ByteArray>): WebResponse<ByteArray> {
        val method = request.method
        val path = segmentPath(request.uri.path)
        val pathVariables = mutableMapOf<String, String>()
        val route = matchRoute(method, path, pathVariables)
                ?: throw FailureResponseException(404, "The method and path is not handled: $method $path")
        val contentType = request.headers.get(CommonHeaders.ContentType).firstOrNull()
                ?.let { MediaType.parse(it) }
                ?: MediaType.APPLICATION_JSON
        val acceptTypes = request.headers.get(CommonHeaders.Accept).map { MediaType.parse(it) }
                .let { if (it.isEmpty()) listOf(MediaType.ALL) else it }
        val deserializer = EntitySerializers.getSerializer(contentType)
                ?: throw FailureResponseException(415, "The media type is not supported: $contentType")
        val (defaultType, defaultSerializer) = EntitySerializers.acceptSerializer(acceptTypes)
                ?: throw FailureResponseException(406, "The media type is not supported: $acceptTypes")
        val request0 = makeRequest(request, deserializer, contentType)
        val context = ContextImpl(route.path, pathVariables, request0)
        val response = route.handler.onRequest(context)
        return makeResponse(response, defaultType, defaultSerializer, acceptTypes)
    }

    private fun segmentPath(path: String): List<String> {
        return path.split('/')
                .drop(1)
                .map { URLHelper.decodePath(it) }
    }

    private fun matchRoute(method: String, path: List<String>, variables: MutableMap<String, String>): Route? {
        routes.values.forEach { route ->
            if (route.accept(method, path, variables)) {
                return route
            }
            variables.clear()
        }
        return null
    }

    private fun makeRequest(request: WebRequest<ByteArray>,
                            deserializer: WebEntitySerializer,
                            mediaType: MediaType): WebRequest<Variant> {
        if (request.entity == null) {
            @Suppress("UNCHECKED_CAST")
            return request as WebRequest<Variant>
        }
        val entity = DataVariant(request.entity, deserializer, mediaType)
        return WebRequest(request.uri, request.method, request.headers, entity)
    }

    private fun makeResponse(response: WebResponse<Any>,
                             defaultType: MediaType,
                             defaultSerializer: WebEntitySerializer,
                             acceptTypes: List<MediaType>): WebResponse<ByteArray> {
        var serializer = defaultSerializer
        val contentType = response.headers.get(CommonHeaders.ContentType).firstOrNull()
                ?.let { MediaType.parse(it) }
                ?: defaultType
        if (contentType !== defaultType) {
            if (!isAcceptable(acceptTypes, contentType)) {
                throw FailureResponseException(406, "The media type is not supported: $contentType")
            }
            serializer = EntitySerializers.getSerializer(contentType)
                    ?: throw FailureResponseException(406, "The media type is not supported: $contentType")
        }
        return when (response.entity) {
            null, is ByteArray -> {
                @Suppress("UNCHECKED_CAST")
                response as WebResponse<ByteArray>
            }
            else -> {
                val entity = serializer.serialize(response.entity, contentType)
                if (contentType === defaultType) {
                    response.headers.set(CommonHeaders.ContentType, listOf(contentType.toString()))
                }
                WebResponse(response.statusCode, response.headers, entity)
            }
        }
    }

    private fun isAcceptable(acceptTypes: List<MediaType>, mediaType: MediaType): Boolean {
        acceptTypes.forEach { a ->
            if (a.isAcceptable(mediaType)) {
                return true
            }
        }
        return false
    }

    private fun addConnectionHeader(response: WebResponse<ByteArray>, request: WebRequest<ByteArray>) {
        val connectionValue = when {
            !config.keepAlive
                    || request.headers.get(CommonHeaders.Connection).firstOrNull() == "close"
                    || response.headers.get(CommonHeaders.Connection).firstOrNull() == "close" -> "close"
            else -> "keep-alive"
        }
        response.headers.set(CommonHeaders.Connection, listOf(connectionValue))
    }

    private fun addContentLengthHeader(response: WebResponse<ByteArray>) {
        if (response.entity == null) {
            response.headers.remove(CommonHeaders.ContentLength)
        } else {
            response.headers.set(CommonHeaders.ContentLength, listOf(response.entity.size.toString()))
        }
    }

    private fun addServerHeader(response: WebResponse<ByteArray>) {
        if (config.serverHeader.isNotEmpty()
                && response.headers.get(CommonHeaders.Server).firstOrNull() == null) {
            response.headers.add(CommonHeaders.Server, config.serverHeader)
        }
    }

    private data class RouteKey(val method: String, val path: String)

    private class Route(
            val method: String,
            val path: String,
            val segments: List<String>,
            val handler: WebHandler
    ) {
        fun accept(method: String, path: List<String>, variables: MutableMap<String, String>): Boolean {
            return acceptMethod(method) && acceptPath(path, variables)
        }

        private fun acceptMethod(value: String): Boolean {
            return method.equals(value, ignoreCase = true)
        }

        private fun acceptPath(value: List<String>, variables: MutableMap<String, String>): Boolean {
            if (segments.size != value.size) {
                return false
            }
            segments.forEachIndexed { i, part ->
                if (part.startsWith(':')) {
                    variables[part.substring(1)] = value[i]
                } else if (part != value[i]) {
                    return false
                }
            }
            return true
        }
    }

    private class ContextImpl(
            override val routePath: String,
            override val pathParams: Map<String, String>,
            override val request: WebRequest<Variant>
    ) : RequestContext {
        override val queryParams = MultiValueMap(true)

        init {
            val queryString = request.uri.rawQuery
            if (queryString != null && queryString.isNotEmpty()) {
                queryString.split('&').forEach { kv ->
                    val (k, v) = kv.split('=', limit = 2)
                    val k0 = URLHelper.decodeQueryString(k)
                    val v0 = URLHelper.decodeQueryString(v)
                    queryParams.add(k0, v0)
                }
            }
        }
    }
}