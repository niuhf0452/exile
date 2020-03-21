package com.github.niuhf0452.exile.config.source

import com.github.niuhf0452.exile.config.Config
import com.github.niuhf0452.exile.config.ConfigSourceLoader
import com.github.niuhf0452.exile.config.ConfigValue
import com.github.niuhf0452.exile.config.impl.Util.log
import java.net.URI

class EnvironmentSource : Config.Source {
    override fun load(): Iterable<ConfigValue> {
        log.info("Load config from environment variables")
        return System.getenv().map { (k, v) ->
            ConfigValue(location, k, v)
        }
    }

    override fun toString(): String {
        return "EnvironmentSource"
    }

    class Loader : ConfigSourceLoader {
        override fun load(uri: URI): Config.Source? {
            if (uri == location) {
                return EnvironmentSource()
            }
            return null
        }
    }

    companion object {
        val location: URI = URI.create("sys://env")
    }
}