package com.github.niuhf0452.exile.common

import kotlinx.serialization.*
import kotlin.reflect.jvm.jvmName

@PublicApi
@Serializable(with = StringList.Serializer::class)
data class StringList(
        private val value: List<String>
) : Iterable<String> {
    override fun iterator(): Iterator<String> {
        return value.iterator()
    }

    override fun toString(): String {
        return value.joinToString(", ")
    }

    fun toList(): List<String> {
        return value
    }

    companion object {
        fun parse(value: String): StringList {
            if (value.isBlank()) {
                return StringList(emptyList())
            }
            val arr = value.split(',')
            if (arr.size == 1) {
                return StringList(listOf(arr[0].trim()))
            }
            val list = arr.asSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toList()
            return StringList(list)
        }
    }

    class Serializer : KSerializer<StringList> {
        override val descriptor: SerialDescriptor = PrimitiveDescriptor(StringList::class.jvmName, PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): StringList {
            return parse(decoder.decodeString())
        }

        override fun serialize(encoder: Encoder, value: StringList) {
            encoder.encodeString(value.toString())
        }
    }
}