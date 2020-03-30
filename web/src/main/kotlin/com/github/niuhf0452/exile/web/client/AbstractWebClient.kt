package com.github.niuhf0452.exile.web.client

import com.github.niuhf0452.exile.web.*
import com.github.niuhf0452.exile.web.internal.DataVariant
import com.github.niuhf0452.exile.web.internal.MultiValueMapImpl
import com.github.niuhf0452.exile.web.serialization.EntitySerializers

abstract class AbstractWebClient : WebClient {
    protected abstract suspend fun backendSend(request: WebRequest<ByteArray>): WebResponse<ByteArray>

    override suspend fun send(request: WebRequest<Any>): WebResponse<Variant> {
        val request0 = addHeaders(request)
        val request1 = request0.mapEntity { e ->
            val contentType = request0.headers.get("Content-Type").first()
            val mediaType = MediaType.parse(contentType)
            val serializer = EntitySerializers.getSerializer(mediaType)
                    ?: throw IllegalArgumentException("The request content type is not supported: $contentType")
            serializer.serialize(e, mediaType)
        }
        val response = backendSend(request1)
        return response.mapEntity { e ->
            val contentType = response.headers.get("Content-Type").firstOrNull()
                    ?: throw IllegalArgumentException("The response has entity but Content-Type header is missing")
            val mediaType = MediaType.parse(contentType)
            val serializer = EntitySerializers.getSerializer(mediaType)
                    ?: throw IllegalArgumentException("The respond content type is not supported: $contentType")
            DataVariant(e, serializer, mediaType)
        }
    }

    private fun addHeaders(request: WebRequest<Any>): WebRequest<Any> {
        if (request.headers.get("Content-Type").firstOrNull() == null) {
            return request.mapHeaders { headers ->
                val headers0 = MultiValueMapImpl(headers, false)
                headers0.add("Content-Type", "application/json")
                headers0
            }
        }
        return request
    }
}