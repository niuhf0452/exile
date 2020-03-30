package com.github.niuhf0452.exile.config.inject

import com.github.niuhf0452.exile.config.Config
import com.github.niuhf0452.exile.config.ConfigMapper
import com.github.niuhf0452.exile.config.Configuration
import com.github.niuhf0452.exile.config.autoConfigure
import com.github.niuhf0452.exile.inject.Injector
import com.github.niuhf0452.exile.inject.InjectorAutoLoader
import com.github.niuhf0452.exile.inject.TypeKey
import com.github.niuhf0452.exile.inject.internal.getQualifiers
import kotlin.reflect.full.findAnnotation

/**
 * This is a Injector Binder for ConfigMapper.Mapping.
 * Don't reference to this class directly, because we don't want hard dependency of inject library.
 */
class ConfigurationBinder(
        private val mapper: ConfigMapper
) : Injector.Binder {
    override fun bind(key: TypeKey, context: Injector.BindingContext) {
        val cls = key.classifier
        if (cls == ConfigMapper.Mapping::class) {
            val mapping = getMapping(key.arguments.first())
            context.bindToInstance(cls.getQualifiers(), mapping)
        } else if (cls.findAnnotation<Configuration>() != null) {
            val mapping = getMapping(key)
            context.bindToProvider(cls.getQualifiers(), Provider(mapping))
        }
    }

    private fun getMapping(key: TypeKey): ConfigMapper.Mapping<*> {
        if (key.arguments.isNotEmpty()) {
            throw IllegalArgumentException("Config type-safe class should not have type parameters: $key")
        }
        val configClass = key.classifier
        return mapper.getMapping(configClass)
    }

    private class Provider<A : Any>(
            private val mapping: ConfigMapper.Mapping<A>
    ) : Injector.Provider {
        override fun getInstance(): Any {
            return mapping.receiver
        }
    }

    class Loader : InjectorAutoLoader {
        override fun getBinders(): List<Injector.Binder> {
            val config = Config.newBuilder().autoConfigure().build()
            val mapper = ConfigMapper.newMapper(config)
            return listOf(ConfigurationBinder(mapper))
        }
    }
}