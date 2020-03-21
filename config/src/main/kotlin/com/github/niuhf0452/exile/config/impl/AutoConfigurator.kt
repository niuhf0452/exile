package com.github.niuhf0452.exile.config.impl

import com.github.niuhf0452.exile.config.*
import com.github.niuhf0452.exile.config.impl.Util.log
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.serializer
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor

class AutoConfigurator {
    private val builder = ConfigImpl.Builder()
    private val appliedProfiles = mutableListOf<String>()

    fun config(configFile: String, activeProfiles: List<String>, overwrite: Config.Source): Config.Source {
        builder.fromResource("/application.init.conf", Config.Order.OVERWRITE)
        builder.fromResource(configFile, Config.Order.OVERWRITE)
        if (File(configFile).exists()) {
            builder.fromFile(configFile, Config.Order.OVERWRITE)
        }
        builder.from(overwrite)
        var config = builder.build()
        config.getList("config.include").forEach { path ->
            builder.fromResource(path, Config.Order.OVERWRITE)
        }
        config = builder.build()
        loadProfiles(config, activeProfiles)
        config = builder.build()
        val composite = CompositeSource.newSource()
        composite.addSource(ListSource(config.toList()))
        config.getMapKeys("config.sources").forEach { key ->
            val sourceConfig = config.getFragment("config.sources.$key", keepPrefix = false)
            if (sourceConfig.getBoolean("enable")) {
                val source = loadSource(sourceConfig)
                composite.addSource(source)
            }
        }
        composite.addSource(ListSource(config.toList()))
        return composite
    }

    private fun loadProfiles(config: Config, activeProfiles: List<String>) {
        loadProfile(config, config)
        activeProfiles.forEach { name ->
            if (name.isNotBlank()) {
                val fragment = config.getFragment("config.profiles.${name.trim()}")
                loadProfile(config, fragment)
            }
        }
        val compact = compactActiveProfiles()
        if (compact.isEmpty()) {
            log.info("Active profiles: default")
        } else {
            log.info("Active profiles: ${compact.joinToString(", ")}")
        }
    }

    private fun loadProfile(config: Config, profileFragment: ConfigFragment) {
        profileFragment.find("config.profiles.inherit")
                ?.asList()
                ?.forEach { name ->
                    log.debug("Apply profile: $name")
                    val fragment = config.getFragment("config.profiles.$name", keepPrefix = false)
                    loadProfile(config, fragment)
                    appliedProfiles.add(name)
                }
        builder.from(ListSource(profileFragment), Config.Order.OVERWRITE)
    }

    private fun compactActiveProfiles(): List<String> {
        val profiles = mutableListOf<String>()
        val reduceSet = mutableSetOf<String>()
        appliedProfiles.asReversed().forEach { name ->
            if (reduceSet.add(name)) {
                profiles.add(name)
            }
        }
        profiles.reverse()
        return profiles
    }

    @OptIn(ImplicitReflectionSerializer::class)
    private fun loadSource(config: ConfigFragment): Config.Source {
        val className = config.getString("class")
        val sourceClass = try {
            Class.forName(className).kotlin
        } catch (ex: Exception) {
            throw ConfigException("Config source can't be loaded: $className", ex)
        }
        if (!sourceClass.isSubclassOf(Config.Source::class)) {
            throw ConfigException("Config source doesn't implement the interface Config.Source: $className")
        }
        val constructor = sourceClass.primaryConstructor
                ?: throw ConfigException("Config source must have a primary constructor: $sourceClass")
        val source = when (constructor.parameters.size) {
            0 -> constructor.call()
            1 -> {
                val parameter = constructor.parameters.first()
                val configClass = parameter.type.classifier as? KClass<*>
                        ?: throw ConfigException("Config source has unsupported constructor parameter type: $parameter")
                val value = try {
                    config.parse(configClass.serializer())
                } catch (ex: Exception) {
                    throw ConfigException("Config source constructor parameter can't be deserialize: $parameter", ex)
                }
                constructor.call(value)
            }
            else -> {
                throw ConfigException("Config source constructor must have zero or one parameter: $constructor")
            }
        }
        return source as Config.Source
    }

    private class ListSource(
            private val values: Iterable<ConfigValue>
    ) : Config.Source {
        override fun load(): Iterable<ConfigValue> {
            return values
        }
    }
}
