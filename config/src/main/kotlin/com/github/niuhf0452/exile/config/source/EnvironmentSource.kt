package com.github.niuhf0452.exile.config.source

import com.github.niuhf0452.exile.config.Config
import com.github.niuhf0452.exile.config.ConfigValue
import com.github.niuhf0452.exile.config.impl.Util.log

class EnvironmentSource : Config.Source {
    override fun load(): Iterable<ConfigValue> {
        log.info("Load config from environment variables")
        return System.getenv().map { (k, v) ->
            ConfigValue(this, k, v)
        }
    }

    override fun toString(): String {
        return "EnvironmentSource"
    }
}