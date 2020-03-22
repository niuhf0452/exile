package com.github.niuhf0452.exile.config

import com.github.niuhf0452.exile.config.impl.*
import com.github.niuhf0452.exile.config.simpleconfig.SimpleConfig
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import java.net.URI
import java.util.*
import kotlin.reflect.KClass

/**
 * ConfigValue keeps a value of configuration.
 *
 * @param location The location of the configuration.
 * @param path The path of value.
 * @param value The value.
 * @since 1.0
 */
data class ConfigValue(val location: URI, val path: String, val value: String) {
    /**
     * Get the value.
     */
    fun asString() = value

    /**
     * Get the value and convert to int.
     */
    fun asInt() = asString().toInt()

    /**
     * Get the value and convert to long.
     */
    fun asLong() = asString().toLong()

    /**
     * Get the value and convert to boolean.
     */
    fun asBoolean() = asString().toBoolean()

    /**
     * Get the value and convert to double.
     */
    fun asDouble() = asString().toDouble()

    /**
     * Get the value and convert to list of string.
     */
    fun asList(): List<String> {
        val text = asString()
        return if (text.isBlank()) {
            emptyList()
        } else {
            text.split(',').map(String::trim)
        }
    }

    override fun toString(): String {
        return "ConfigValue($path = $value, location = $location)"
    }
}

/**
 * A fragment is a piece of configuration.
 * The fragment is static. Event the configuration is reloaded, the fragment won't be changed.
 *
 * @since 1.0
 */
interface ConfigFragment : Iterable<ConfigValue> {
    /**
     * Get the config value at certain path, or null if there's no value matched the path.
     *
     * @param path The path to query.
     * @return The config value.
     */
    fun find(path: String): ConfigValue?

    /**
     * Get the config value at certain path, throw exception if not found.
     *
     * @param path The path to query.
     * @return The config value
     * @throws ConfigException If the path/value doesn't exist.
     */
    @Throws(ConfigException::class)
    fun get(path: String): ConfigValue {
        return find(path)
                ?: throw ConfigException("Config doesn't exist: $path")
    }

    /**
     * Get the value in string.
     */
    fun getString(path: String) = get(path).asString()

    /**
     * Get the value and convert to int.
     */
    fun getInt(path: String) = get(path).asInt()

    /**
     * Get the value and convert to long.
     */
    fun getLong(path: String) = get(path).asLong()

    /**
     * Get the value and convert to boolean.
     */
    fun getBoolean(path: String) = get(path).asBoolean()

    /**
     * Get the value and convert to double.
     */
    fun getDouble(path: String) = get(path).asDouble()

    /**
     * Get the value and convert to list of string.
     */
    fun getList(path: String) = get(path).asList()

    /**
     * Get a fragment of the configuration.
     * Note that, the fragment is kind of collection, so that the method never throws exception,
     * even the path doesn't exist. But it can returns a fragment without any config value.
     *
     * @param path The path of fragment.
     * @param keepPrefix True, if want to keep the path in returned fragment.
     *                   False, if want to trip the path in returned fragment.
     * @return The fragment.
     */
    fun getFragment(path: String, keepPrefix: Boolean = false): ConfigFragment

    /**
     * This method works for map like data structure. It returns the direct sub path of a path.
     * For example, if there are following values:
     *
     * ```
     * db.mysql.username = user
     * db.mysql.password = pass
     * db.redis.index = 0
     * ```
     *
     * The call of `getMapKeys("config")` will return: mysql, redis.
     *
     * @param path The path to query.
     * @return The set of sub paths under `[path]`.
     */
    fun getMapKeys(path: String): Set<String>
}

/**
 * Config the primary API of config library.
 * It keeps a configuration which is consistent by values in the memory.
 * It supports [reload] the configuration in runtime,
 * and [getSnapshot] which means to get a consistent view of configuration.
 *
 * @since 1.0
 */
interface Config : ConfigFragment {
    fun getSnapshot(): ConfigFragment

    fun reload()

    /**
     * The SPI to integrate config with other systems.
     *
     * @since 1.0
     */
    interface Source {
        /**
         * Fetch and return a list of config values.
         */
        fun load(): Iterable<ConfigValue>
    }

    enum class Order {
        /**
         * Always use the value from current source even previous added sources
         * also provide the value at certain path.
         */
        OVERWRITE,

        /**
         * Use the value from current source only if the previous added sources
         * can't provide the value at certain path.
         */
        FALLBACK
    }

    /**
     * A resolver to resolve placeholder to value.
     * For a value in the configuration, we can use placeholder to refer to another value.
     * For example:
     *
     * ```
     * a = ${b}
     * b = 123
     * ```
     *
     * The `${xxx}` is a placeholder.
     *
     * The config library doesn't support to customize the placeholder, but it does support
     * to extend the ways how to get the value from placeholder by the SPI [ValueResolver].
     *
     * After Config loads configurations, it try to resolve every placeholder by calling the method [resolve],
     * and pass in the placeholder value which is between '${' and '}'.
     *
     * @since 1.0
     */
    interface ValueResolver {
        fun resolve(value: String, context: ValueResolverContext): String?
    }

    /**
     * A context used in [ValueResolver.resolve].
     *
     * @see ValueResolver
     * @since 1.0
     */
    interface ValueResolverContext {
        fun find(path: String): String?
    }

    /**
     * The builder of Config class.
     *
     * @since 1.0
     */
    interface Builder {
        /**
         * Add a source.
         */
        fun from(source: Source, order: Order = Order.OVERWRITE): Builder

        /**
         * Add a source which loads values from the simple config string.
         */
        fun fromString(content: String, order: Order = Order.OVERWRITE): Builder

        /**
         * Add a source which loads values from the properties object.
         */
        fun fromProperties(props: Properties, order: Order = Order.OVERWRITE): Builder

        /**
         * Add a source which loads values from resource files.
         * Note that there may be multiple files with the same path in the classpath.
         * This method will load all the files.
         *
         * The file can be multiple types. The type of file is indicated by extension name.
         * * .conf - A simple config file.
         * * .properties - A properties file.
         * * .* - Auto search any files with supported extensions.
         *        If multiple files found, all of them will be loaded. But the priority is not guaranteed.
         */
        fun fromResource(path: String, order: Order = Order.OVERWRITE): Builder

        /**
         * Add a source which loads values from file. It's similar as [fromResource], but the file can be external.
         */
        fun fromFile(url: URI, order: Order = Order.OVERWRITE): Builder

        /**
         * Add a source which loads values from file.
         */
        fun fromFile(path: String, order: Order = Order.OVERWRITE): Builder

        /**
         * Add a source which loads values from file from environment variables.
         */
        fun fromEnvironment(): Builder

        /**
         * Add a source which loads values from system properties.
         */
        fun fromSystemProperties(): Builder

        /**
         * Add a source which loads values from a URI. The URI is parsed by [ConfigSourceLoader].
         * This method is generally provides a extensible way to load configuration.
         *
         * The built-in URI pattern are:
         * * sys://env - Load from environment variables.
         * * sys://properties - Load from system properties.
         * * properties://mem - Load from properties object. The properties is passed in by query string.
         * * simple://mem?content= - Load from simple config string. The simple config string is passed
         *                           in by content parameter in query string.
         * * file:/ - Load from file. The path must be a valid file path.
         * * vault:/http://localhost? - Load from Vault. The path of URI is the url of Vault.
         *                              The parameters of Vault are passed in by query string.
         *                              See [VaultConfig].
         */
        fun from(uri: URI, order: Order = Order.OVERWRITE): Builder

        /**
         * Add a [ValueResolver].
         */
        fun addResolver(resolver: ValueResolver): Builder

        fun build(): Config
    }

    companion object {
        fun newBuilder(): Builder {
            return ConfigImpl.Builder()
        }
    }
}

/**
 * ConfigSourceLoader is the adapter interface of ServiceLoader.
 * Since it's used for ServiceLoader, the implement class should give a non-arg constructor.
 *
 * @since 1.0
 */
interface ConfigSourceLoader {
    fun load(uri: URI): Config.Source?
}

class ConfigException(message: String, exception: Exception? = null)
    : RuntimeException(message, exception)

/**
 * ConfigMapper is a type-safe API of Config.
 *
 * The type-safe config features is based on kotlin serialization.
 * So make sure the type-safe config class is serializable.
 */
interface ConfigMapper {
    /**
     * Add a mapping.
     */
    fun <T : Any> addMapping(path: String, cls: KClass<T>, deserializer: DeserializationStrategy<T>)

    /**
     * Add a mapping.
     */
    fun <T : Any> addMapping(path: String, cls: KClass<T>)

    /**
     * Add a mapping.
     */
    fun <T : Any> addMapping(cls: KClass<T>)

    /**
     * Get a type-safe config object by type.
     */
    fun <A : Any> get(cls: KClass<A>): A {
        return getMapping(cls).receiver
    }

    /**
     * Get a mapping by type.
     */
    fun <A : Any> getMapping(cls: KClass<A>): Mapping<A>

    /**
     * Get all mappings.
     */
    fun mappings(): List<Mapping<*>>

    /**
     * Reload the configuration and related type-safe objects.
     */
    fun reload()

    /**
     * A Mapping is a mapping from config path to the type-safe config object.
     *
     * @since 1.0
     */
    interface Mapping<T : Any> {
        /**
         * The path in configuration.
         */
        val path: String

        /**
         * The class of type-safe config object.
         */
        val receiverClass: KClass<T>

        /**
         * The deserializer of [receiverClass].
         */
        val deserializer: DeserializationStrategy<T>

        /**
         * The type-safe config object.
         */
        val receiver: T
    }

    companion object {
        fun newMapper(config: Config): ConfigMapper {
            return ConfigMapperImpl(config)
        }
    }
}

/**
 * This annotation is a hint to [ConfigMapper] to let it knows the path of the type-safe config class.
 * If it is used with Injector API, then the annotated class can be auto wired.
 *
 * @since 1.0
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Configuration(val value: String)

/**
 * Auto load sources from a bootstrap configuration file.
 *
 * @since 1.0
 */
fun Config.Builder.autoConfigure(
        configFile: String = Util.getConfigFile(),
        activeProfiles: List<String> = Util.getActiveProfiles(),
        overwrite: Config.Source = EmptyConfig.EmptySource
): Config.Builder {
    return from(AutoConfigurator().config(configFile, activeProfiles, overwrite))
}

/**
 * Deserialize type-safe config object from config fragment.
 *
 * @since 1.0
 */
fun <T> ConfigFragment.parse(serial: DeserializationStrategy<T>): T {
    return SimpleConfig().parse(this, serial)
}

/**
 * Serialize type-safe config object to config fragment.
 *
 * @since 1.0
 */
fun <T> Config.Companion.toConfig(serial: SerializationStrategy<T>, data: T, location: URI = EmptyConfig.location): ConfigFragment {
    return SimpleConfig().toConfig(data, serial, location)
}