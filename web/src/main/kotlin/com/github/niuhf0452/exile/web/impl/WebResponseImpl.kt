package com.github.niuhf0452.exile.web.impl

import com.github.niuhf0452.exile.web.WebHeaders
import com.github.niuhf0452.exile.web.WebResponse

class WebResponseImpl<E>(
        override val statusCode: Int,
        override val headers: WebHeaders,
        override val entity: E
) : WebResponse<E> {
    class Builder : WebResponse.Builder {
        private var statusCode = 0
        private val headers = WebHeadersImpl()

        override fun statusCode(value: Int): WebResponse.Builder {
            statusCode = value
            return this
        }

        override fun headers(value: WebHeaders): WebResponse.Builder {
            headers.clear()
            value.forEach { name ->
                headers.set(name, value.get(name))
            }
            return this
        }

        override fun setHeaders(value: Map<String, String>): WebResponse.Builder {
            headers.set(value)
            return this
        }

        override fun addHeader(name: String, value: String): WebResponse.Builder {
            headers.add(name, value)
            return this
        }

        override fun setHeader(name: String, value: Iterable<String>): WebResponse.Builder {
            headers.set(name, value)
            return this
        }

        override fun removeHeader(name: String): WebResponse.Builder {
            headers.remove(name)
            return this
        }

        override fun <T> entity(value: T): WebResponse<T> {
            return WebResponseImpl(statusCode, headers, value)
        }
    }
}