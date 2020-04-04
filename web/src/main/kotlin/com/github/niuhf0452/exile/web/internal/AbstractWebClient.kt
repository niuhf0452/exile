package com.github.niuhf0452.exile.web.internal

import com.github.niuhf0452.exile.web.*
import com.github.niuhf0452.exile.web.serialization.EntitySerializers
import kotlin.reflect.KClass

abstract class AbstractWebClient : WebClient {
    protected abstract val interceptors: InterceptorList
    protected abstract suspend fun backendSend(request: WebRequest<ByteArray>): WebResponse<ByteArray>

    override suspend fun send(request: WebRequest<Any>): WebResponse<Variant> {
        val request0 = makeRequest(request)
        val response = interceptors.handleRequest(request0) {
            backendSend(it)
        }
        return makeResponse(response)
    }

    override fun addInterceptor(interceptor: WebInterceptor) {
        interceptors.add(interceptor)
    }

    override fun removeInterceptor(cls: KClass<*>) {
        interceptors.remove(cls)
    }

    private fun makeRequest(request: WebRequest<Any>): WebRequest<ByteArray> {
        val headers = MultiValueMap(request.headers, false)
        var entity: ByteArray? = null
        if (request.entity == null) {
            if (headers.get(CommonHeaders.ContentLength).firstOrNull() != null
                    || headers.get(CommonHeaders.ContentType).firstOrNull() != null) {
                headers.remove(CommonHeaders.ContentType)
                headers.remove(CommonHeaders.ContentLength)
            }
        } else {
            var mediaType = headers.get(CommonHeaders.ContentType).firstOrNull()
                    ?.let { MediaType.parse(it) }
            if (mediaType == null) {
                mediaType = MediaType.APPLICATION_JSON
                headers.add(CommonHeaders.ContentType, MediaType.APPLICATION_JSON.text)
            }
            val serializer = EntitySerializers.getSerializer(mediaType)
                    ?: throw IllegalArgumentException("The request content type is not supported: $mediaType")
            entity = serializer.serialize(request.entity, mediaType)
            headers.add(CommonHeaders.ContentLength, entity.size.toString())
        }
        // always create a new instance, because interceptor may change it,
        // but we don't want to reflect to the input request.
        return WebRequest(request.uri, request.method, headers, entity)
    }

    private fun makeResponse(response: WebResponse<ByteArray>): WebResponse<Variant> {
        if (response.entity == null) {
            return WebResponse(response.statusCode, response.headers, null)
        }
        val contentType = response.headers.get(CommonHeaders.ContentType).firstOrNull()
                ?: throw IllegalArgumentException("The response has entity but Content-Type header is missing")
        val mediaType = MediaType.parse(contentType)
        val serializer = EntitySerializers.getSerializer(mediaType)
                ?: throw IllegalArgumentException("The respond content type is not supported: $contentType")
        val data = DataVariant(response.entity, serializer, mediaType)
        return WebResponse(response.statusCode, response.headers, data)
    }
}