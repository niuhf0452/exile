package com.github.niuhf0452.exile.config.source

import com.github.niuhf0452.exile.config.Config
import com.github.niuhf0452.exile.config.ConfigSourceLoader
import com.github.niuhf0452.exile.config.ConfigValue
import com.github.niuhf0452.exile.config.internal.Util.configPathRegex
import com.github.niuhf0452.exile.config.internal.Util.log
import java.net.URI

class SystemPropertiesSource : Config.Source {
    override fun load(): Iterable<ConfigValue> {
        log.info("Load config from system properties")
        val values = mutableListOf<ConfigValue>()
        System.getProperties().forEach { (k, v) ->
            if (k != null && v != null) {
                val path = k.toString()
                val value = v.toString()
                if (configPathRegex.matches(path)) {
                    values.add(ConfigValue(location, path, value))
                }
            }
        }
        return values
    }

    override fun toString(): String {
        return "SystemPropertiesSource"
    }

    class Loader : ConfigSourceLoader {
        override fun load(uri: URI): Config.Source? {
            if (uri == location) {
                return SystemPropertiesSource()
            }
            return null
        }
    }

    companion object {
        val location: URI = URI.create("sys://properties")
    }
}