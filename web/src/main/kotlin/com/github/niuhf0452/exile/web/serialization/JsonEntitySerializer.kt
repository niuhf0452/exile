package com.github.niuhf0452.exile.web.serialization

import com.github.niuhf0452.exile.web.MediaType
import com.github.niuhf0452.exile.web.WebEntitySerializer
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerialModule
import kotlin.reflect.KClass

@OptIn(UnstableDefault::class)
class JsonEntitySerializer(
        private val module: SerialModule
) : WebEntitySerializer {
    override val mediaTypes: List<MediaType> = listOf(MediaType.APPLICATION_JSON)

    override fun serialize(data: Any, mediaType: MediaType): ByteArray {
        return Json.stringify(module.getSerializer(data), data).toByteArray()
    }

    override fun deserialize(data: ByteArray, cls: KClass<*>, mediaType: MediaType): Any {
        return Json.parse(module.getDeserializer(cls), data.toString(Charsets.UTF_8))
    }

    class Factory : WebEntitySerializer.Factory {
        override fun createSerializer(module: SerialModule): WebEntitySerializer {
            return JsonEntitySerializer(module)
        }
    }
}