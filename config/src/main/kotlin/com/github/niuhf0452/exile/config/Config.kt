package com.github.niuhf0452.exile.config

import com.github.niuhf0452.exile.config.impl.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import java.net.URL
import java.util.*
import kotlin.reflect.KClass

data class ConfigValue(val source: Config.Source, val path: String, val value: String) {
    fun asString() = value

    fun asInt() = asString().toInt()

    fun asLong() = asString().toLong()

    fun asBoolean() = asString().toBoolean()

    fun asDouble() = asString().toDouble()

    fun asList(): List<String> {
        val text = asString()
        return if (text.isBlank()) {
            emptyList()
        } else {
            text.split(',').map(String::trim)
        }
    }

    override fun toString(): String {
        return "ConfigValue($path = $value, source = $source)"
    }
}

interface ConfigFragment : Iterable<ConfigValue> {
    fun find(path: String): ConfigValue?

    fun get(path: String): ConfigValue {
        return find(path)
                ?: throw ConfigException("Config doesn't exist: $path")
    }

    fun getString(path: String) = get(path).asString()

    fun getInt(path: String) = get(path).asInt()

    fun getLong(path: String) = get(path).asLong()

    fun getBoolean(path: String) = get(path).asBoolean()

    fun getDouble(path: String) = get(path).asDouble()

    fun getList(path: String) = get(path).asList()

    fun getFragment(path: String, keepPrefix: Boolean = false): ConfigFragment
}

interface Config : ConfigFragment {
    fun getSnapshot(): ConfigFragment

    fun reload()

    interface Source {
        fun load(): Iterable<ConfigValue>
    }

    enum class Order {
        OVERWRITE, FALLBACK
    }

    interface ValueResolver {
        fun resolve(value: String, context: ValueResolverContext): String?
    }

    interface ValueResolverContext {
        fun find(path: String): String?
    }

    interface Builder {
        fun from(source: Source, order: Order = Order.OVERWRITE): Builder

        fun fromString(content: String, order: Order = Order.OVERWRITE): Builder

        fun fromProperties(props: Properties, order: Order = Order.OVERWRITE): Builder

        fun fromResource(path: String, order: Order = Order.OVERWRITE): Builder

        fun fromFile(url: URL, order: Order = Order.OVERWRITE): Builder

        fun fromFile(path: String, order: Order = Order.OVERWRITE): Builder

        fun fromEnvironment(): Builder

        fun fromSystemProperties(): Builder

        fun addResolver(resolver: ValueResolver): Builder

        fun build(): Config
    }

    companion object {
        fun newBuilder(): Builder {
            return ConfigImpl.Builder()
        }
    }
}

class ConfigException(message: String, exception: Exception? = null)
    : RuntimeException(message, exception)

interface ConfigMapper {
    fun <A : Any> get(cls: KClass<A>): A
    fun get(path: String): Any
    fun mappings(): List<Mapping<*>>
    fun reload()

    interface Mapping<T : Any> {
        val path: String
        val receiverClass: KClass<T>
        val deserializer: DeserializationStrategy<T>
        val receiver: Any
    }

    interface Builder {
        fun config(config: Config): Builder

        fun <T : Any> addMapping(path: String, cls: KClass<T>, deserializer: DeserializationStrategy<T>): Builder

        fun <T : Any> addMapping(path: String, cls: KClass<T>): Builder

        fun <T : Any> addMapping(cls: KClass<T>): Builder

        fun build(): ConfigMapper
    }

    companion object {
        fun newBuilder(): Builder {
            return ConfigMapperImpl.Builder()
        }
    }
}

@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Configuration(val value: String)

fun Config.Builder.autoConfigure(
        configFile: String = Util.getConfigFile(),
        activeProfiles: List<String> = Util.getActiveProfiles(),
        overwrite: Config.Source = EmptyConfig.EmptySource
): Config.Builder {
    return from(AutoConfigurator().config(configFile, activeProfiles, overwrite))
}

fun <T> ConfigFragment.parse(serial: DeserializationStrategy<T>): T {
    return SimpleConfig().parse(this, serial)
}

fun <T> Config.Companion.toConfig(serial: SerializationStrategy<T>, data: T): ConfigFragment {
    return SimpleConfig().toConfig(data, serial)
}