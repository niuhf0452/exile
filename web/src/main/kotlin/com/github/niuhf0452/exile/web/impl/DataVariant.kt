package com.github.niuhf0452.exile.web.impl

import com.github.niuhf0452.exile.web.FailureResponseException
import com.github.niuhf0452.exile.web.MediaType
import com.github.niuhf0452.exile.web.Variant
import com.github.niuhf0452.exile.web.WebEntitySerializer
import kotlin.reflect.KClass

class DataVariant(
        private val data: ByteArray,
        private val deserializer: WebEntitySerializer,
        private val mediaType: MediaType
) : Variant {
    override val isEmpty: Boolean
        get() = false

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> convertTo(cls: KClass<T>): T {
        return deserializer.deserialize(data, cls, mediaType) as T
    }
}

object EmptyVariant : Variant {
    override val isEmpty: Boolean
        get() = true

    override fun <T : Any> convertTo(cls: KClass<T>): T {
        throw FailureResponseException(400, "Expect entity")
    }
}