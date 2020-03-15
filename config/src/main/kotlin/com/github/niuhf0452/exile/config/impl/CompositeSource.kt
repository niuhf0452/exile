package com.github.niuhf0452.exile.config.impl

import com.github.niuhf0452.exile.config.Config
import com.github.niuhf0452.exile.config.ConfigValue
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList

class CompositeSource(
        private val sources: MutableList<Config.Source>
) : Config.Source, Iterable<Config.Source> {
    override fun load(): Iterable<ConfigValue> {
        return sources.fold(mutableListOf()) { all, s ->
            all.addAll(s.load())
            all
        }
    }

    override fun iterator(): Iterator<Config.Source> {
        return sources.iterator()
    }

    fun addSource(source: Config.Source, order: Config.Order = Config.Order.OVERWRITE) {
        when (order) {
            Config.Order.FALLBACK -> sources.add(0, source)
            Config.Order.OVERWRITE -> sources.add(source)
        }
    }

    override fun toString(): String {
        return "CompositeSource(${sources.joinToString(", ")})"
    }

    companion object {
        fun newSource(): CompositeSource {
            return CompositeSource(mutableListOf())
        }

        fun threadSafeSource(): CompositeSource {
            return CompositeSource(CopyOnWriteArrayList())
        }
    }
}
