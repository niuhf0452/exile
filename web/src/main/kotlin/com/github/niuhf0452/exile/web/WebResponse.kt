package com.github.niuhf0452.exile.web

import com.github.niuhf0452.exile.common.Fluent
import com.github.niuhf0452.exile.common.PublicApi

@PublicApi
class WebResponse<out E>(
        val statusCode: Int,
        val headers: MultiValueMap,
        val entity: E?
) {
    @PublicApi
    class Builder<E> {
        private var statusCode = 0
        private val headers = MultiValueMap(false)
        private var entity: Any? = null

        @Fluent
        fun statusCode(value: Int): Builder<E> {
            statusCode = value
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

        fun build(): WebResponse<E> {
            @Suppress("UNCHECKED_CAST")
            return WebResponse(statusCode, headers, entity as E?)
        }
    }

    companion object {
        @PublicApi
        fun newBuilder(): Builder<Nothing> {
            return Builder()
        }
    }
}