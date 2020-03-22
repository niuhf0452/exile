package com.github.niuhf0452.exile.config.impl

import com.github.niuhf0452.exile.config.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.serializer
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

class ConfigMapperImpl(
        private val config: Config
) : ConfigMapper {
    private val mappings = ConcurrentHashMap<KClass<*>, Mapping<*>>()

    override fun <T : Any> addMapping(path: String, cls: KClass<T>, deserializer: DeserializationStrategy<T>) {
        if (mappings.putIfAbsent(cls, Mapping(path, cls, deserializer)) != null) {
            throw ConfigException("Config mapping of class already exists: $cls")
        }
    }

    @OptIn(ImplicitReflectionSerializer::class)
    override fun <T : Any> addMapping(path: String, cls: KClass<T>) {
        addMapping(path, cls, cls.serializer())
    }

    override fun <T : Any> addMapping(cls: KClass<T>) {
        val a = cls.findAnnotation<Configuration>()
                ?: throw ConfigException("Config mapping class should be annotated with @Configuration: $cls")
        addMapping(a.value, cls)
    }

    @OptIn(ImplicitReflectionSerializer::class)
    override fun <A : Any> getMapping(cls: KClass<A>): ConfigMapper.Mapping<A> {
        val mapping = mappings.computeIfAbsent(cls) {
            val path = cls.findAnnotation<Configuration>()
                    ?.value
                    ?: throw ConfigException("Config mapping class should be annotated with @Configuration: $cls")
            Mapping(path, cls, cls.serializer())
        }
        @Suppress("UNCHECKED_CAST")
        return mapping as ConfigMapper.Mapping<A>
    }

    override fun mappings(): List<ConfigMapper.Mapping<*>> {
        return mappings.values.toList()
    }

    override fun reload() {
        config.reload()
        mappings.values.forEach { it.reload() }
    }

    private inner class Mapping<T : Any>(
            override val path: String,
            override val receiverClass: KClass<T>,
            override val deserializer: DeserializationStrategy<T>
    ) : ConfigMapper.Mapping<T> {
        private val listeners = ConcurrentHashMap.newKeySet<ConfigMapper.Listener<T>>()

        override var receiver: T = load()

        override fun addListener(listener: ConfigMapper.Listener<T>) {
            listeners.add(listener)
        }

        override fun toString(): String {
            return "Mapping($path, $receiver)"
        }

        private fun load(): T {
            val fragment = config.getFragment(path)
            return fragment.parse(deserializer)
        }

        fun reload() {
            val newValue = load()
            if (receiver != newValue) {
                receiver = newValue
                listeners.forEach { it.onUpdate(newValue) }
            }
        }
    }
}