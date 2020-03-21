package com.github.niuhf0452.exile.config.impl

import com.github.niuhf0452.exile.config.Config
import com.github.niuhf0452.exile.config.ConfigFragment
import com.github.niuhf0452.exile.config.ConfigValue
import com.github.niuhf0452.exile.config.impl.Util.log
import java.io.File

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
        loadProfile(config, config)
        activeProfiles.forEach { name ->
            if (name.isNotBlank()) {
                val fragment = config.getFragment("config.profiles.${name.trim()}")
                loadProfile(config, fragment)
            }
        }
        log.info("Active profiles: ${reduceAppliedProfiles().joinToString(", ")}")
        return ListSource(builder.build().toList())
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

    private fun reduceAppliedProfiles(): List<String> {
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

    private class ListSource(
            private val values: Iterable<ConfigValue>
    ) : Config.Source {
        override fun load(): Iterable<ConfigValue> {
            return values
        }
    }
}
