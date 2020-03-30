package com.github.niuhf0452.exile.web

class WebResponse<out E>(
        val statusCode: Int,
        val headers: MultiValueMap,
        val entity: E?
) {
    class Builder<E> {
        private var statusCode = 0
        private val headers = MultiValueMap(false)
        private var entity: Any? = null

        fun statusCode(value: Int): Builder<E> {
            statusCode = value
            return this
        }

        fun headers(value: MultiValueMap): Builder<E> {
            headers.clear()
            value.forEach { name ->
                headers.set(name, value.get(name))
            }
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

        fun build(): WebResponse<E> {
            @Suppress("UNCHECKED_CAST")
            return WebResponse(statusCode, headers, entity as E?)
        }
    }

    companion object {
        fun newBuilder(): Builder<Nothing> {
            return Builder()
        }
    }
}