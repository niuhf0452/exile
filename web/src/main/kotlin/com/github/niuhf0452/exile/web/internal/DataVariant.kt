package com.github.niuhf0452.exile.web.internal

import com.github.niuhf0452.exile.web.MediaType
import com.github.niuhf0452.exile.web.Variant
import com.github.niuhf0452.exile.web.WebEntitySerializer
import kotlin.reflect.KClass

class DataVariant(
        private val data: ByteArray,
        private val deserializer: WebEntitySerializer,
        private val mediaType: MediaType
) : Variant {
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> convertTo(cls: KClass<T>): T {
        return deserializer.deserialize(data, cls, mediaType) as T
    }
}