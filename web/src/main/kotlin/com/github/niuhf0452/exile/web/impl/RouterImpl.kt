package com.github.niuhf0452.exile.web.impl

import com.github.niuhf0452.exile.web.*
import com.github.niuhf0452.exile.web.Responses.NotAcceptable
import com.github.niuhf0452.exile.web.Responses.NotFound
import com.github.niuhf0452.exile.web.Responses.UnsupportedMediaType
import com.github.niuhf0452.exile.web.serialization.EntitySerializers
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap

class RouterImpl(
        private val config: WebServer.Config
) : Router {
    private val routes = ConcurrentHashMap<RouteKey, Route>()
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
        val pathVariables = mutableMapOf<String, String>()
        val route = matchRoute(method, path, pathVariables)
                ?: return NotFound
        val consumes = request.headers.get("Content-Type").firstOrNull()
                ?.let { MediaType.parse(it) }
                ?: MediaType.APPLICATION_JSON
        val produces = request.headers.get("Accept").firstOrNull()
                ?.let { MediaType.parse(it) }
                ?: MediaType.ALL
        val deserializer = EntitySerializers.getSerializer(consumes)
                ?: return UnsupportedMediaType
        val serializer = EntitySerializers.getSerializer(produces)
                ?: return NotAcceptable
        val response = try {
            val request0 = makeRequest(request, deserializer, consumes)
            val context = ContextImpl(route.path, pathVariables, request0)
            val response = route.handler.onRequest(context)
            makeResponse(response, serializer, produces)
        } catch (ex: DirectResponseException) {
            ex.response
        } catch (ex: Exception) {
            exceptionHandler.handle(ex)
        }
        // make sure connection header is always set correctly.
        return response.mapHeaders {
            addConnectionHeader(request, response)
        }
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
        return request.mapEntity { data ->
            DataVariant(data, deserializer, mediaType)
        }
    }

    private fun makeResponse(response: WebResponse<Any>,
                             produceSerializer: WebEntitySerializer,
                             produces: MediaType): WebResponse<ByteArray> {
        val contentType = response.headers.get("Content-Type").firstOrNull()
                ?.let { MediaType.parse(it) }
                ?: produces
        val serializer0 = when {
            contentType == produces -> produceSerializer
            produces.isAcceptable(contentType) ->
                EntitySerializers.getSerializer(contentType)
                        ?: throw DirectResponseException(NotAcceptable)
            else -> throw DirectResponseException(NotAcceptable)
        }
        return response.mapEntity { entity ->
            when (entity) {
                is ByteArray -> entity
                else -> serializer0.serialize(entity, contentType)
            }
        }
    }

    private fun addConnectionHeader(request: WebRequest<ByteArray>, response: WebResponse<Any>): MultiValueMap {
        val connectionValue = when {
            !config.keepAlive
                    || request.headers.get("Connection").firstOrNull() == "close"
                    || response.headers.get("Connection").firstOrNull() == "close" -> "close"
            else -> "keep-alive"
        }
        val headers = MultiValueMapImpl(response.headers, false)
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
            override val pathParams: Map<String, String>,
            override val request: WebRequest<Variant>
    ) : RequestContext {
        override val queryParams: MultiValueMap = run {
            val queryString = request.uri.rawQuery
            if (queryString == null || queryString.isEmpty()) {
                MultiValueMap.Empty
            } else {
                val map = MultiValueMapImpl(true)
                queryString.split('&').forEach { kv ->
                    val (k, v) = kv.split('=', limit = 2)
                    map.add(URLDecoder.decode(k, Charsets.UTF_8), URLDecoder.decode(v, Charsets.UTF_8))
                }
                map
            }
        }
    }
}