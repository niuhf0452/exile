package com.github.niuhf0452.exile.yaml

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.github.niuhf0452.exile.web.MediaType
import com.github.niuhf0452.exile.web.WebEntitySerializer
import com.github.niuhf0452.exile.web.serialization.getDeserializer
import com.github.niuhf0452.exile.web.serialization.getSerializer
import kotlinx.serialization.modules.SerialModule
import kotlin.reflect.KClass

class YamlEntitySerializer(
        private val module: SerialModule
) : WebEntitySerializer {
    private val yaml = Yaml(module, YamlConfiguration(
            encodeDefaults = true,
            strictMode = false,
            extensionDefinitionPrefix = null
    ))

    override val mediaTypes: List<MediaType> = listOf(MediaType.TEXT_YAML, MediaType.APPLICATION_X_YAML)

    override fun serialize(data: Any, mediaType: MediaType): ByteArray {
        return yaml.stringify(module.getSerializer(data), data).toByteArray()
    }

    override fun deserialize(data: ByteArray, cls: KClass<*>, mediaType: MediaType): Any {
        return yaml.parse(module.getDeserializer(cls), data.toString(Charsets.UTF_8))
    }

    class Factory : WebEntitySerializer.Factory {
        override fun createSerializer(module: SerialModule): WebEntitySerializer {
            return YamlEntitySerializer(module)
        }
    }
}