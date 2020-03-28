package com.github.niuhf0452.exile.web.serialization

import com.github.niuhf0452.exile.web.MediaType
import com.github.niuhf0452.exile.web.WebEntitySerializer
import com.github.niuhf0452.exile.web.WebResponseException
import com.github.niuhf0452.exile.web.Responses
import kotlinx.serialization.modules.SerialModule
import kotlin.reflect.KClass

class TextEntitySerializer : WebEntitySerializer {
    override fun acceptConsumes(mediaType: MediaType): Boolean {
        return MediaType.TEXT_PLAIN.isAcceptable(mediaType)
                || MediaType.TEXT_HTML.isAcceptable(mediaType)
    }

    override fun acceptProduces(mediaType: MediaType): Boolean {
        return mediaType.isAcceptable(MediaType.TEXT_PLAIN)
                || mediaType.isAcceptable(MediaType.TEXT_HTML)
    }

    override fun serialize(data: Any, mediaType: MediaType): ByteArray {
        if (data !is String) {
            throw WebResponseException(Responses.NotAcceptable)
        }
        return data.toByteArray(mediaType.charset)
    }

    override fun deserialize(data: ByteArray, cls: KClass<*>, mediaType: MediaType): Any {
        if (cls != String::class) {
            throw IllegalArgumentException("class should be string: $cls")
        }
        return data.toString(mediaType.charset)
    }

    class Factory : WebEntitySerializer.Factory {
        override fun createSerializer(module: SerialModule): WebEntitySerializer {
            return TextEntitySerializer()
        }
    }
}