package com.github.niuhf0452.exile.config.impl

import com.github.niuhf0452.exile.config.Config
import com.github.niuhf0452.exile.config.ConfigException
import com.github.niuhf0452.exile.config.ConfigFragment
import com.github.niuhf0452.exile.config.ConfigValue
import java.net.URL
import java.util.*

class ConfigImpl(
        private val source: Config.Source,
        private val resolver: Config.ValueResolver
) : Config {
    @Volatile
    private var fragment: ConfigFragment = load()

    override fun find(path: String): ConfigValue? {
        return fragment.find(path)
    }

    override fun getFragment(path: String, keepPrefix: Boolean): ConfigFragment {
        return fragment.getFragment(path, keepPrefix)
    }

    override fun getSnapshot(): ConfigFragment {
        return fragment
    }

    override fun iterator(): Iterator<ConfigValue> {
        return fragment.iterator()
    }

    override fun reload() {
        fragment = load()
    }

    private fun load(): ConfigFragment {
        val map = TreeMap<String, ConfigValue>()
        source.load().forEach { value ->
            map[value.path] = value
        }
        val resolved = ResolverContext(map).resolveAll()
        return ConfigFragmentImpl(resolved)
    }

    private inner class ResolverContext(
            private val map: Map<String, ConfigValue>
    ) : Config.ValueResolverContext {
        private val placeHolderResolver = PlaceHolderResolver()
        private val resolved = TreeMap<String, ConfigValue>()
        private val backtrace = Stack<String>()

        override fun find(path: String): String? {
            if (backtrace.contains(path)) {
                throw ConfigException("Config cyclically depends")
            }
            var value = resolved[path]
            if (value != null) {
                return value.asString()
            }
            value = map[path]
                    ?: return null
            value = resolve(value)
            resolved[path] = value
            return value.asString()
        }

        private fun resolve(value: ConfigValue): ConfigValue {
            backtrace.push(value.path)
            val str = value.asString()
            val resolved = placeHolderResolver.resolve(str, this)
            backtrace.pop()
            if (resolved == null || resolved == str) {
                return value
            }
            return value.copy(value = resolved)
        }

        fun resolveAll(): SortedMap<String, ConfigValue> {
            try {
                map.forEach { (k, v) ->
                    if (!resolved.containsKey(k)) {
                        resolved[k] = resolve(v)
                    }
                }
            } catch (ex: Exception) {
                throw ConfigException("Config can't be resolved:\n- ${backtrace.joinToString("\n- ")}", ex)
            }
            return resolved
        }
    }

    private inner class PlaceHolderResolver : Config.ValueResolver {
        private val pattern = "\\$\\{([^}]+)}".toRegex()

        override fun resolve(value: String, context: Config.ValueResolverContext): String? {
            if (!pattern.containsMatchIn(value)) {
                return null
            }
            val sb = StringBuilder()
            var start = 0
            while (start < value.length - 1) {
                val m = pattern.find(value, start)
                        ?: break
                sb.append(value.substring(start, m.range.first))
                val placeholder = m.groupValues[1]
                val resolved = resolver.resolve(placeholder, context)
                        ?: throw ConfigException("Config placeholder can't be resolved: $value")
                sb.append(resolved)
                start = m.range.last + 1
            }
            sb.append(value.substring(start))
            return sb.toString()
        }
    }

    class Builder : Config.Builder {
        private val source = CompositeSource.newSource()
        private val resolver = CompositeValueResolver()

        override fun from(source: Config.Source, order: Config.Order): Config.Builder {
            this.source.addSource(source, order)
            return this
        }

        override fun fromString(content: String, order: Config.Order): Config.Builder {
            return from(SimpleConfigSource(content), order)
        }

        override fun fromProperties(props: Properties, order: Config.Order): Config.Builder {
            return from(PropertiesSource(props), order)
        }

        override fun fromResource(path: String, order: Config.Order): Config.Builder {
            val composite = CompositeSource.newSource()
            val path0 = if (path.startsWith('/')) {
                path.substring(1)
            } else {
                path
            }
            if (path0.endsWith(".*")) {
                val file = path0.substring(0, path0.length - 2)
                FileSource.supportedFileTypes.forEach { type ->
                    loadResource(composite, "$file.${type.ext}")
                }
            } else {
                // check file type
                FileSource.getFileType(URL("file:/${path0}"))
                loadResource(composite, path0)
            }
            return from(composite, order)
        }

        private fun loadResource(composite: CompositeSource, path: String) {
            for (url in javaClass.classLoader.getResources(path)) {
                composite.addSource(FileSource(url), Config.Order.FALLBACK)
            }
        }

        override fun fromFile(url: URL, order: Config.Order): Config.Builder {
            return from(FileSource(url), order)
        }

        override fun fromFile(path: String, order: Config.Order): Config.Builder {
            val path0 = when {
                path.startsWith("~/") -> "${System.getProperty("user.home")}/${path.substring(2)}"
                path.startsWith("/") -> path
                else -> "${System.getProperty("user.dir")}/$path"
            }
            return fromFile(URL("file:$path0"), order)
        }

        override fun fromEnvironment(): Config.Builder {
            return from(EnvironmentSource())
        }

        override fun fromSystemProperties(): Config.Builder {
            return from(SystemPropertiesSource())
        }

        override fun addResolver(resolver: Config.ValueResolver): Config.Builder {
            this.resolver.addResolver(resolver)
            return this
        }

        override fun build(): Config {
            return ConfigImpl(source, resolver)
        }
    }
}