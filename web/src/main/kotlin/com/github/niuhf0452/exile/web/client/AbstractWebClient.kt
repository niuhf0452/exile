package com.github.niuhf0452.exile.web.client

import com.github.niuhf0452.exile.web.*
import com.github.niuhf0452.exile.web.internal.DataVariant
import com.github.niuhf0452.exile.web.serialization.EntitySerializers

abstract class AbstractWebClient : WebClient {
    protected abstract suspend fun backendSend(request: WebRequest<ByteArray>): WebResponse<ByteArray>

    override suspend fun send(request: WebRequest<Any>): WebResponse<Variant> {
        val request0 = if (request.entity == null) {
            @Suppress("UNCHECKED_CAST")
            request as WebRequest<ByteArray>
        } else {
            var headers = request.headers
            var mediaType = headers.get("Content-Type").firstOrNull()
                    ?.let { MediaType.parse(it) }
            if (mediaType == null) {
                headers = MultiValueMap(headers, false)
                headers.add("Content-Type", "application/json")
                mediaType = MediaType.APPLICATION_JSON
            }
            val serializer = EntitySerializers.getSerializer(mediaType)
                    ?: throw IllegalArgumentException("The request content type is not supported: $mediaType")
            val data = serializer.serialize(request.entity, mediaType)
            WebRequest(request.uri, request.method, headers, data)
        }
        val response = backendSend(request0)
        if (response.entity == null) {
            return WebResponse(response.statusCode, response.headers, null)
        }
        val contentType = response.headers.get("Content-Type").firstOrNull()
                ?: throw IllegalArgumentException("The response has entity but Content-Type header is missing")
        val mediaType = MediaType.parse(contentType)
        val serializer = EntitySerializers.getSerializer(mediaType)
                ?: throw IllegalArgumentException("The respond content type is not supported: $contentType")
        val data = DataVariant(response.entity, serializer, mediaType)
        return WebResponse(response.statusCode, response.headers, data)
    }
}