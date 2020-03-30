package com.github.niuhf0452.exile.config.internal

import com.github.niuhf0452.exile.config.Config
import com.github.niuhf0452.exile.config.ConfigFragment
import com.github.niuhf0452.exile.config.ConfigValue
import java.net.URI

object EmptyConfig : Config {
    val location: URI = URI.create("empty://mem")

    override fun find(path: String): ConfigValue? {
        return null
    }

    override fun getFragment(path: String, keepPrefix: Boolean): ConfigFragment {
        return this
    }

    override fun getMapKeys(path: String): Set<String> {
        return emptySet()
    }

    override fun getSnapshot(): ConfigFragment {
        return this
    }

    override fun iterator(): Iterator<ConfigValue> {
        return emptyList<ConfigValue>().iterator()
    }

    override fun reload() = Unit

    object EmptySource : Config.Source {
        override fun load(): Iterable<ConfigValue> {
            return emptyList()
        }

        override fun toString(): String {
            return "EmptySource"
        }
    }
}