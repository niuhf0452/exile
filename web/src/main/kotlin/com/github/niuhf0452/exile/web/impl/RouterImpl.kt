package com.github.niuhf0452.exile.web.impl

import com.github.niuhf0452.exile.web.*
import com.github.niuhf0452.exile.web.Responses.NotAcceptable
import com.github.niuhf0452.exile.web.Responses.NotFound
import com.github.niuhf0452.exile.web.Responses.UnsupportedMediaType
import kotlinx.serialization.modules.SerialModule
import java.net.URLDecoder
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class RouterImpl(
        private val config: WebServer.Config,
        module: SerialModule
) : Router {
    private val routes = ConcurrentHashMap<RouteKey, Route>()
    private val serializers = ServiceLoader.load(WebEntitySerializer.Factory::class.java)
            .map { it.createSerializer(module) }
    private var exceptionHandler: WebExceptionHandler = DefaultExceptionHandler()

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
        val method = request.method
        val path = segmentPath(request.uri.path)
        val variables = mutableMapOf<String, String>()
        val route = matchRoute(method, path, variables)
                ?: return NotFound
        val consumes = request.headers.get("Content-Type").firstOrNull()
                ?.let { MediaType.parse(it) }
                ?: MediaType.APPLICATION_JSON
        val produces = request.headers.get("Accept").firstOrNull()
                ?.let { MediaType.parse(it) }
                ?: MediaType.ALL
        val deserializer = serializers.find { it.acceptConsumes(consumes) }
                ?: return UnsupportedMediaType
        val serializer = serializers.find { it.acceptProduces(produces) }
                ?: return NotAcceptable
        val context = ContextImpl(route.path, variables)
        val response = try {
            val response = route.handler.onRequest(context, makeRequest(request, deserializer, consumes))
            makeResponse(response, serializer, produces)
        } catch (ex: Exception) {
            exceptionHandler.handle(ex)
        }
        // make sure connection header is always set correctly.
        val headers = addConnectionHeader(request, response)
        return WebResponseImpl(response.statusCode, headers, response.entity)
    }

    override fun setExceptionHandler(handler: WebExceptionHandler) {
        exceptionHandler = when (handler) {
            is DefaultExceptionHandler -> handler
            else -> SafeExceptionHandler(handler)
        }
    }

    private fun segmentPath(path: String): List<String> {
        return path.split('/')
                .drop(1)
                .map { URLDecoder.decode(it, Charsets.UTF_8) }
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
        val e = request.entity
        val entity = when {
            e.isEmpty() -> EmptyVariant
            else -> DataVariant(e, deserializer, mediaType)
        }
        return WebRequestImpl(request.uri, request.method, request.headers, entity)
    }

    private fun makeResponse(response: WebResponse<Any>,
                             produceSerializer: WebEntitySerializer,
                             produces: MediaType): WebResponse<ByteArray> {
        val entity = serializeEntity(response, produceSerializer, produces)
        return WebResponseImpl(response.statusCode, response.headers, entity)
    }

    private fun serializeEntity(response: WebResponse<Any>,
                                produceSerializer: WebEntitySerializer,
                                produces: MediaType): ByteArray {
        val contentType = response.headers.get("Content-Type").firstOrNull()
                ?.let { MediaType.parse(it) }
                ?: produces
        val serializer0 = when {
            contentType == produces -> produceSerializer
            produces.isAcceptable(contentType) ->
                serializers.find { it.acceptProduces(contentType) }
                        ?: throw WebResponseException(NotAcceptable)
            else -> throw WebResponseException(NotAcceptable)
        }
        return when (val e = response.entity) {
            Unit -> emptyByteArray
            is ByteArray -> e
            else -> serializer0.serialize(e, contentType)
        }
    }

    private fun addConnectionHeader(request: WebRequest<ByteArray>, response: WebResponse<Any>): WebHeaders {
        val connectionValue = when {
            !config.keepAlive
                    || request.headers.get("Connection").firstOrNull() == "close"
                    || response.headers.get("Connection").firstOrNull() == "close" -> "close"
            else -> "keep-alive"
        }
        val headers = WebHeadersImpl(response.headers)
        headers.set("Connection", listOf(connectionValue))
        return headers
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
            override val pathVariables: Map<String, String>
    ) : RequestContext

    private class DataVariant(
            private val data: ByteArray,
            private val deserializer: WebEntitySerializer,
            private val mediaType: MediaType
    ) : Variant {
        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> convertTo(cls: KClass<T>): T {
            return deserializer.deserialize(data, cls, mediaType) as T
        }
    }

    private object EmptyVariant : Variant {
        override fun <T : Any> convertTo(cls: KClass<T>): T {
            throw WebResponseException(400, "Expect entity")
        }
    }
}