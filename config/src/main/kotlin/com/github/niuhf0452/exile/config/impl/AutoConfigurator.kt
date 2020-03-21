package com.github.niuhf0452.exile.config.impl

import com.github.niuhf0452.exile.config.Config
import com.github.niuhf0452.exile.config.ConfigFragment
import com.github.niuhf0452.exile.config.ConfigValue
import com.github.niuhf0452.exile.config.impl.Util.log
import java.io.File
import java.net.URI
import java.net.URLEncoder

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
        val output = ConfigImpl.Builder()
        output.from(ListSource(config.toList()), Config.Order.OVERWRITE)
        loadSources(output, config)
        return output.source()
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

    private fun loadSources(builder: Config.Builder, config: ConfigFragment) {
        config.getMapKeys("config.sources").map { key ->
            config.getFragment("config.sources.$key", keepPrefix = false)
        }.sortedBy { sourceConfig ->
            sourceConfig.find("order")?.asInt() ?: 0
        }.forEach { sourceConfig ->
            val enable = sourceConfig.find("enable")
                    ?.asBoolean()
                    ?: true
            if (enable) {
                builder.from(makeSourceUri(sourceConfig), Config.Order.OVERWRITE)
            }
        }
    }

    private fun makeSourceUri(config: ConfigFragment): URI {
        val fragment = config.getFragment("query", keepPrefix = false)
        val qs = fragment.joinToString("&") { (_, k, v) ->
            "${URLEncoder.encode(k, Charsets.UTF_8)}=${URLEncoder.encode(v, Charsets.UTF_8)}"
        }
        return URI.create(config.getString("uri") + "?" + qs)
    }

    private class ListSource(
            private val values: Iterable<ConfigValue>
    ) : Config.Source {
        override fun load(): Iterable<ConfigValue> {
            return values
        }
    }
}
