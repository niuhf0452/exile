package com.github.niuhf0452.exile.web.internal

import com.github.niuhf0452.exile.web.MultiValueMap
import com.github.niuhf0452.exile.web.WebRequest
import java.net.URI
import java.net.URLEncoder

class WebRequestImpl<E>(
        override val uri: URI,
        override val method: String,
        override val headers: MultiValueMap,
        override val entity: E
) : WebRequest<E> {
    override val hasEntity: Boolean
        get() = true

    override fun mapHeaders(f: (MultiValueMap) -> MultiValueMap): WebRequest<E> {
        return WebRequestImpl(uri, method, f(headers), entity)
    }

    override fun <A> mapEntity(f: (E) -> A): WebRequest<A> {
        return WebRequestImpl(uri, method, headers, f(entity))
    }

    class NoEntity(
            override val uri: URI,
            override val method: String,
            override val headers: MultiValueMap
    ) : WebRequest<Nothing> {
        override val hasEntity: Boolean
            get() = false

        override val entity: Nothing
            get() = throw IllegalStateException("No entity")

        override fun mapHeaders(f: (MultiValueMap) -> MultiValueMap): WebRequest<Nothing> {
            return NoEntity(uri, method, f(headers))
        }

        override fun <A> mapEntity(f: (Nothing) -> A): WebRequest<A> {
            return this
        }
    }

    class Builder(
            private val uri: String
    ) : WebRequest.Builder<Any> {
        private var method = ""
        private val pathParams = mutableMapOf<String, String>()
        private val queryParams = mutableListOf<Pair<String, String>>()
        private var headers = MultiValueMapImpl(false)
        private var entity: Any? = null

        override fun method(value: String): WebRequest.Builder<Any> {
            method = value
            return this
        }

        override fun setPathParam(name: String, value: String): WebRequest.Builder<Any> {
            pathParams[name] = value
            return this
        }

        override fun addQueryParam(name: String, value: String): WebRequest.Builder<Any> {
            queryParams.add(name to value)
            return this
        }

        override fun setHeaders(value: Map<String, String>): WebRequest.Builder<Any> {
            headers.set(value)
            return this
        }

        override fun addHeader(name: String, value: String): WebRequest.Builder<Any> {
            headers.add(name, value)
            return this
        }

        override fun setHeader(name: String, value: Iterable<String>): WebRequest.Builder<Any> {
            headers.set(name, value)
            return this
        }

        override fun removeHeader(name: String): WebRequest.Builder<Any> {
            headers.remove(name)
            return this
        }

        override fun <T> entity(value: T): WebRequest.Builder<T> {
            entity = value
            @Suppress("UNCHECKED_CAST")
            return this as WebRequest.Builder<T>
        }

        override fun noEntity(): WebRequest.Builder<Nothing> {
            entity = null
            @Suppress("UNCHECKED_CAST")
            return this as WebRequest.Builder<Nothing>
        }

        override fun build(): WebRequest<Any> {
            if (method.isEmpty()) {
                throw IllegalArgumentException("The method is not set")
            }
            val headers0 = if (headers.isEmpty) MultiValueMap.Empty else headers
            val entity0 = entity
            return if (entity0 == null) {
                NoEntity(getURI(), method, headers0)
            } else {
                WebRequestImpl(getURI(), method, headers0, entity0)
            }
        }

        private fun getURI(): URI {
            var start = uri.indexOf("://")
            if (start < 0) {
                throw IllegalArgumentException("URI is invalid: $uri")
            }
            start = uri.indexOf('/', start + 3)
            if (start < 0) {
                return URI.create(uri)
            }
            val sb = StringBuilder()
            sb.append(uri.substring(0, start))
            uri.substring(start).split('/').joinTo(sb, "/") { p ->
                var value = p
                if (value.startsWith(':')) {
                    value = pathParams[value.substring(1)]
                            ?: throw IllegalArgumentException("Path parameter not set: $value")
                }
                encodePath(value)
            }
            if (queryParams.isNotEmpty()) {
                sb.append('?')
                queryParams.forEach { (k, v) ->
                    sb.append(encodePath(k)).append('=').append(encodePath(v)).append('&')
                }
                sb.setLength(sb.length - 1)
            }
            return URI.create(sb.toString())
        }

        private fun encodePath(s: String): String {
            return URLEncoder.encode(s, Charsets.UTF_8).replace("\\+", "%20")
        }
    }
}