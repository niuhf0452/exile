package com.github.niuhf0452.exile.config.source

import com.github.niuhf0452.exile.common.Bytes
import com.github.niuhf0452.exile.common.PublicApi
import com.github.niuhf0452.exile.common.URLHelper
import com.github.niuhf0452.exile.config.Config
import com.github.niuhf0452.exile.config.ConfigSourceLoader
import com.github.niuhf0452.exile.config.ConfigValue
import com.github.niuhf0452.exile.config.internal.Util.configPathRegex
import com.github.niuhf0452.exile.config.internal.Util.log
import java.io.ByteArrayOutputStream
import java.net.URI
import java.security.MessageDigest
import java.util.*

@PublicApi
class PropertiesSource(
        private val properties: Properties
) : Config.Source {
    private val values = mutableListOf<ConfigValue>()
    private val hash: String

    init {
        val out = ByteArrayOutputStream()
        val writer = out.writer()
        properties.forEach { (k, v) ->
            if (k != null && v != null) {
                val path = k.toString()
                val value = v.toString()
                if (!configPathRegex.matches(path)) {
                    throw IllegalArgumentException("Config path is invalid: $path")
                }
                values.add(ConfigValue(location, path, value))
                writer.write(path)
                writer.write("=")
                writer.write(value)
                writer.write("\n")
            }
        }
        val md = MessageDigest.getInstance("SHA1")
        hash = md.digest(out.toByteArray()).let(Bytes::toHex)
        out.close()
    }

    override fun load(): Iterable<ConfigValue> {
        val content = TreeMap(properties).entries.joinToString("\n  ") { (k, v) -> "$k = $v" }
        log.info("Load config from properties:\n  $content\n")
        return values
    }

    override fun toString(): String {
        return "PropertiesSource($hash)"
    }

    class Loader : ConfigSourceLoader {
        override fun load(uri: URI): Config.Source? {
            if (uri.scheme == location.scheme) {
                val map = uri.rawQuery
                        ?.let(URLHelper::parseQueryString)
                        ?: emptyMap()
                val props = Properties()
                props.putAll(map)
                return PropertiesSource(props)
            }
            return null
        }
    }

    companion object {
        val location: URI = URI.create("properties://mem")
    }
}