package com.github.niuhf0452.exile.web.impl

import com.github.niuhf0452.exile.web.MultiValueMap
import com.github.niuhf0452.exile.web.WebResponse

class WebResponseImpl<E>(
        override val statusCode: Int,
        override val headers: MultiValueMap,
        override val entity: E
) : WebResponse<E> {
    override val hasEntity: Boolean
        get() = true

    override fun mapHeaders(f: (MultiValueMap) -> MultiValueMap): WebResponse<E> {
        return WebResponseImpl(statusCode, f(headers), entity)
    }

    override fun <A> mapEntity(f: (E) -> A): WebResponse<A> {
        return WebResponseImpl(statusCode, headers, f(entity))
    }

    class NoEntity(
            override val statusCode: Int,
            override val headers: MultiValueMap
    ) : WebResponse<Nothing> {
        override val hasEntity: Boolean
            get() = false
        override val entity: Nothing
            get() = throw IllegalStateException("No entity")

        override fun mapHeaders(f: (MultiValueMap) -> MultiValueMap): WebResponse<Nothing> {
            return NoEntity(statusCode, f(headers))
        }

        override fun <A> mapEntity(f: (Nothing) -> A): WebResponse<A> {
            return this
        }
    }

    class Builder : WebResponse.Builder<Any> {
        private var statusCode = 0
        private val headers = MultiValueMapImpl(false)
        private var entity: Any? = null

        override fun statusCode(value: Int): WebResponse.Builder<Any> {
            statusCode = value
            return this
        }

        override fun headers(value: MultiValueMap): WebResponse.Builder<Any> {
            headers.clear()
            value.forEach { name ->
                headers.set(name, value.get(name))
            }
            return this
        }

        override fun setHeaders(value: Map<String, String>): WebResponse.Builder<Any> {
            headers.set(value)
            return this
        }

        override fun addHeader(name: String, value: String): WebResponse.Builder<Any> {
            headers.add(name, value)
            return this
        }

        override fun setHeader(name: String, value: Iterable<String>): WebResponse.Builder<Any> {
            headers.set(name, value)
            return this
        }

        override fun removeHeader(name: String): WebResponse.Builder<Any> {
            headers.remove(name)
            return this
        }

        override fun <T> entity(value: T): WebResponse.Builder<T> {
            entity = value
            @Suppress("UNCHECKED_CAST")
            return this as WebResponse.Builder<T>
        }

        override fun noEntity(): WebResponse.Builder<Nothing> {
            entity = null
            @Suppress("UNCHECKED_CAST")
            return this as WebResponse.Builder<Nothing>
        }

        override fun build(): WebResponse<Any> {
            val headers = if (headers.isEmpty) MultiValueMap.Empty else headers
            val entity0 = entity
            return if (entity0 == null) {
                NoEntity(statusCode, headers)
            } else {
                WebResponseImpl(statusCode, headers, entity0)
            }
        }
    }
}