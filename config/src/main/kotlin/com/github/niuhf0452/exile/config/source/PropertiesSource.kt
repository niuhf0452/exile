package com.github.niuhf0452.exile.config.source

import com.github.niuhf0452.exile.config.Config
import com.github.niuhf0452.exile.config.ConfigValue
import com.github.niuhf0452.exile.config.impl.Util.configPathRegex
import com.github.niuhf0452.exile.config.impl.Util.log
import com.github.niuhf0452.exile.config.impl.Util.toHexString
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.*

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
                values.add(ConfigValue(this, path, value))
                writer.write(path)
                writer.write("=")
                writer.write(value)
                writer.write("\n")
            }
        }
        val md = MessageDigest.getInstance("SHA1")
        hash = md.digest(out.toByteArray()).toHexString()
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
}