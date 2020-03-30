package com.github.niuhf0452.exile.web

import java.net.URI
import java.net.URLEncoder

class WebRequest<out E>(
        val uri: URI,
        val method: String,
        val headers: MultiValueMap,
        val entity: E?
) {
    class Builder<E>(
            private val method: String,
            private val uri: String
    ) {
        private val pathParams = mutableMapOf<String, String>()
        private val queryParams = mutableListOf<Pair<String, String>>()
        private var headers = MultiValueMap(false)
        private var entity: Any? = null

        fun setPathParam(name: String, value: String): Builder<E> {
            pathParams[name] = value
            return this
        }

        fun addQueryParam(name: String, value: String): Builder<E> {
            queryParams.add(name to value)
            return this
        }

        fun setHeaders(value: Map<String, String>): Builder<E> {
            headers.set(value)
            return this
        }

        fun addHeader(name: String, value: String): Builder<E> {
            headers.add(name, value)
            return this
        }

        fun setHeader(name: String, value: Iterable<String>): Builder<E> {
            headers.set(name, value)
            return this
        }

        fun removeHeader(name: String): Builder<E> {
            headers.remove(name)
            return this
        }

        fun <T> entity(value: T): Builder<T> {
            entity = value
            @Suppress("UNCHECKED_CAST")
            return this as Builder<T>
        }

        fun noEntity(): Builder<Nothing> {
            entity = null
            @Suppress("UNCHECKED_CAST")
            return this as Builder<Nothing>
        }

        fun build(): WebRequest<E> {
            if (method.isEmpty()) {
                throw IllegalArgumentException("The method is not set")
            }
            @Suppress("UNCHECKED_CAST")
            return WebRequest(getURI(), method, headers, entity as E?)
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

    companion object {
        fun newBuilder(method: String, uri: String): Builder<Nothing> {
            return Builder(method, uri)
        }
    }
}