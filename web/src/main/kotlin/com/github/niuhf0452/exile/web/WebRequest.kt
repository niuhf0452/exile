package com.github.niuhf0452.exile.web

import com.github.niuhf0452.exile.common.Fluent
import com.github.niuhf0452.exile.common.PublicApi
import com.github.niuhf0452.exile.common.URLHelper
import java.net.URI

@PublicApi
class WebRequest<out E>(
        val uri: URI,
        val method: String,
        val headers: MultiValueMap,
        val entity: E?
) {
    @PublicApi
    class Builder<E>(
            private val method: String,
            _uri: String
    ) {
        private val uri: String
        private val pathParams = mutableMapOf<String, String>()
        private val queryParams = mutableListOf<Pair<String, String>>()
        private var headers = MultiValueMap(false)
        private var entity: Any? = null

        init {
            val i = _uri.indexOf('?')
            if (i < 0) {
                uri = _uri
            } else {
                uri = _uri.substring(0, i)
                val q = _uri.substring(i + 1)
                setQueryString(q)
            }
        }

        @Fluent
        fun setQueryString(value: String): Builder<E> {
            queryParams.clear()
            if (value.isNotBlank()) {
                value.split('&').forEach { kv ->
                    val arr = kv.split('=', limit = 2)
                    if (arr.size == 2) {
                        val (k, v) = arr
                        addQueryParam(URLHelper.decodeQueryString(k), URLHelper.decodeQueryString(v))
                    }
                }
            }
            return this
        }

        @Fluent
        fun setPathParam(name: String, value: String): Builder<E> {
            pathParams[name] = value
            return this
        }

        @Fluent
        fun addQueryParam(name: String, value: String): Builder<E> {
            queryParams.add(name to value)
            return this
        }

        @Fluent
        fun addHeader(name: String, value: String): Builder<E> {
            headers.add(name, value)
            return this
        }

        @Fluent
        fun setHeader(name: String, value: Iterable<String>): Builder<E> {
            headers.set(name, value)
            return this
        }

        @Fluent
        fun removeHeader(name: String): Builder<E> {
            headers.remove(name)
            return this
        }

        @Fluent
        fun <T> entity(value: T): Builder<T> {
            entity = value
            @Suppress("UNCHECKED_CAST")
            return this as Builder<T>
        }

        @Fluent
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
                URLHelper.encodePath(value)
            }
            if (queryParams.isNotEmpty()) {
                sb.append('?')
                queryParams.forEach { (k, v) ->
                    sb.append(URLHelper.encodeQueryString(k))
                            .append('=')
                            .append(URLHelper.encodeQueryString(v))
                            .append('&')
                }
                sb.setLength(sb.length - 1)
            }
            return URI.create(sb.toString())
        }
    }

    companion object {
        @PublicApi
        fun newBuilder(method: String, uri: String): Builder<Nothing> {
            return Builder(method, uri)
        }
    }
}