package com.github.niuhf0452.exile.common

import kotlinx.serialization.*
import java.net.URI

@Serializable(with = URIString.Serializer::class)
data class URIString(
        private val value: URI
) {
    override fun toString(): String {
        return value.toString()
    }

    fun toURI(): URI {
        return value
    }

    companion object {
        fun parse(value: String): URIString {
            return URIString(URI(value))
        }
    }

    class Serializer : KSerializer<URIString> {
        override val descriptor: SerialDescriptor = PrimitiveDescriptor("Duration", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): URIString {
            return parse(decoder.decodeString())
        }

        override fun serialize(encoder: Encoder, value: URIString) {
            encoder.encodeString(value.toString())
        }
    }
}