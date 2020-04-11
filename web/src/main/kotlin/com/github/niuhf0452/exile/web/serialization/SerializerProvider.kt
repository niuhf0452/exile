package com.github.niuhf0452.exile.web.serialization

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.SerialModule
import kotlinx.serialization.modules.getContextualOrDefault
import kotlin.reflect.KClass

interface SerializerProvider {
    fun getSerializer(): SerializationStrategy<*>
}

@OptIn(ImplicitReflectionSerializer::class)
fun <T : Any> SerialModule.getSerializer(value: T): SerializationStrategy<T> {
    if (value is SerializerProvider) {
        @Suppress("UNCHECKED_CAST")
        return value.getSerializer() as SerializationStrategy<T>
    }
    return getContextualOrDefault(value)
}

@OptIn(ImplicitReflectionSerializer::class)
fun <T : Any> SerialModule.getDeserializer(cls: KClass<T>): DeserializationStrategy<T> {
    return getContextualOrDefault(cls)
}