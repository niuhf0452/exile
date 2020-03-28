package com.github.niuhf0452.exile.web.impl

import com.github.niuhf0452.exile.web.WebHeaders
import com.github.niuhf0452.exile.web.WebRequest
import java.net.URI
import java.net.URLEncoder

class WebRequestImpl<E>(
        override val uri: URI,
        override val method: String,
        override val headers: WebHeaders,
        override val entity: E
) : WebRequest<E> {
    class Builder(
            private val uri: String
    ) : WebRequest.Builder {
        private var method = ""
        private val pathParams = mutableMapOf<String, String>()
        private val queryParams = mutableListOf<Pair<String, String>>()
        private var headers = WebHeadersImpl()

        override fun method(value: String): WebRequest.Builder {
            method = value
            return this
        }

        override fun setPathParam(name: String, value: String): WebRequest.Builder {
            pathParams[name] = value
            return this
        }

        override fun addQueryParam(name: String, value: String): WebRequest.Builder {
            queryParams.add(name to value)
            return this
        }

        override fun setHeaders(value: Map<String, String>): WebRequest.Builder {
            headers.set(value)
            return this
        }

        override fun addHeader(name: String, value: String): WebRequest.Builder {
            headers.add(name, value)
            return this
        }

        override fun setHeader(name: String, value: Iterable<String>): WebRequest.Builder {
            headers.set(name, value)
            return this
        }

        override fun removeHeader(name: String): WebRequest.Builder {
            headers.remove(name)
            return this
        }

        override fun <T> entity(value: T): WebRequest<T> {
            return WebRequestImpl(getURI(), method, headers, value)
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