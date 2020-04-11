package com.github.niuhf0452.exile.core.internal

import com.github.niuhf0452.exile.core.ErrorCode
import com.github.niuhf0452.exile.web.WebEntityTransformer
import com.github.niuhf0452.exile.web.serialization.SerializerProvider
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializer

class ResponseStandardTransformer : WebEntityTransformer {
    override fun transform(value: Any?): Any? {
        if (value == null) {
            return null
        }
        return StandardResponse(ErrorCode.OK, value)
    }

    @Serializable
    data class StandardResponse<A : Any>(val code: ErrorCode, val value: A) : SerializerProvider {
        @OptIn(ImplicitReflectionSerializer::class)
        override fun getSerializer(): SerializationStrategy<*> {
            return serializer(value::class.serializer())
        }
    }
}