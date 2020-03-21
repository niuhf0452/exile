package com.github.niuhf0452.exile.config.impl

import com.github.niuhf0452.exile.config.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

class ConfigMapperImpl(
        private val config: Config,
        private val mappings: List<Mapping<*>>
) : ConfigMapper {
    private val byPath = mutableMapOf<String, Mapping<*>>()
    private val byClass = mutableMapOf<KClass<*>, Mapping<*>>()

    init {
        mappings.forEach { m ->
            if (byPath.putIfAbsent(m.path, m) != null) {
                throw IllegalArgumentException("Config name conflict: ${m.path}")
            }
            if (byClass.putIfAbsent(m.receiverClass, m) != null) {
                throw IllegalArgumentException("Config class conflict: ${m.receiverClass}")
            }
        }
        load()
    }

    override fun <A : Any> get(cls: KClass<A>): A {
        val mapping = byClass[cls]
                ?: throw ConfigException("Config for class doesn't exist: $cls")
        @Suppress("UNCHECKED_CAST")
        return mapping.receiver as A
    }

    override fun get(path: String): Any {
        val mapping = byPath[path]
                ?: throw ConfigException("Config mapping doesn't exist: $path")
        return mapping.receiver
    }

    override fun mappings(): List<ConfigMapper.Mapping<*>> {
        return mappings
    }

    override fun reload() {
        config.reload()
        load()
    }

    private fun load() {
        mappings.forEach { mapping ->
            val fragment = config.getFragment(mapping.path)
            mapping.setConfig(fragment)
        }
    }

    class Mapping<T : Any>(
            override val path: String,
            override val receiverClass: KClass<T>,
            override val deserializer: DeserializationStrategy<T>
    ) : ConfigMapper.Mapping<T> {
        override var receiver: Any = Unit

        override fun toString(): String {
            return "Mapping($path, $receiver)"
        }

        fun setConfig(fragment: ConfigFragment) {
            receiver = fragment.parse(deserializer)
        }
    }

    class Builder : ConfigMapper.Builder {
        private var config: Config = EmptyConfig
        private val mappings = mutableListOf<Mapping<*>>()

        override fun config(config: Config): ConfigMapper.Builder {
            this.config = config
            return this
        }

        override fun <T : Any> addMapping(path: String, cls: KClass<T>, deserializer: DeserializationStrategy<T>): ConfigMapper.Builder {
            mappings.add(Mapping(path, cls, deserializer))
            return this
        }

        @OptIn(ImplicitReflectionSerializer::class)
        override fun <T : Any> addMapping(path: String, cls: KClass<T>): ConfigMapper.Builder {
            return addMapping(path, cls, cls.serializer())
        }

        override fun <T : Any> addMapping(cls: KClass<T>): ConfigMapper.Builder {
            val a = cls.findAnnotation<Configuration>()
                    ?: throw ConfigException("Config mapping class should be annotated with @Configuration: $cls")
            return addMapping(a.value, cls)
        }

        override fun build(): ConfigMapper {
            return ConfigMapperImpl(config, mappings)
        }
    }
}