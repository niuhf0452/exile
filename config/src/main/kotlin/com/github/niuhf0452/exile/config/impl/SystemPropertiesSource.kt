package com.github.niuhf0452.exile.config.impl

import com.github.niuhf0452.exile.config.Config
import com.github.niuhf0452.exile.config.ConfigValue
import com.github.niuhf0452.exile.config.impl.Util.configPathRegex
import com.github.niuhf0452.exile.config.impl.Util.log

class SystemPropertiesSource : Config.Source {
    override fun load(): Iterable<ConfigValue> {
        log.info("Load config from system properties")
        val values = mutableListOf<ConfigValue>()
        System.getProperties().forEach { (k, v) ->
            if (k != null && v != null) {
                val path = k.toString()
                val value = v.toString()
                if (configPathRegex.matches(path)) {
                    values.add(ConfigValue(this, path, value))
                }
            }
        }
        return values
    }

    override fun toString(): String {
        return "SystemPropertiesSource"
    }
}