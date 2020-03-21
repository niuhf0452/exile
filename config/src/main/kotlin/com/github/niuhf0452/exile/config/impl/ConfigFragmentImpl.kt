package com.github.niuhf0452.exile.config.impl

import com.github.niuhf0452.exile.config.ConfigFragment
import com.github.niuhf0452.exile.config.ConfigValue
import java.util.*

class ConfigFragmentImpl(
        private val map: SortedMap<String, ConfigValue>
) : ConfigFragment {
    override fun find(path: String): ConfigValue? {
        return map[path]
    }

    override fun getFragment(path: String, keepPrefix: Boolean): ConfigFragment {
        if (path.isEmpty()) {
            throw IllegalArgumentException()
        }
        val subMapView = map.subMap("$path.", "$path${'.' + 1}")
        if (keepPrefix) {
            return ConfigFragmentImpl(subMapView)
        }
        val subMap = TreeMap<String, ConfigValue>()
        val prefixLen = path.length + 1
        subMapView.forEach { (k, v) ->
            val newPath = k.substring(prefixLen)
            subMap[newPath] = v.copy(path = newPath)
        }
        return ConfigFragmentImpl(subMap)
    }

    override fun getMapKeys(path: String): Set<String> {
        val fragment = if (path.isEmpty()) {
            this
        } else {
            getFragment(path, keepPrefix = true)
        }
        val start = if (path.isEmpty()) 0 else path.length + 1
        return fragment.mapTo(mutableSetOf()) { v ->
            var end = v.path.indexOf('.', start)
            if (end < 0) {
                end = v.path.length
            }
            v.path.substring(start, end)
        }
    }

    override fun iterator(): Iterator<ConfigValue> {
        return map.values.iterator()
    }
}