package com.github.niuhf0452.exile.config.impl

import com.github.niuhf0452.exile.config.*
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

class ConfigMapperImpl(
        private val config: Config,
        mappingClasses: Map<String, KClass<*>>
) : ConfigMapper {
    private val byName = mutableMapOf<String, MappingImpl>()
    private val byClass = mutableMapOf<KClass<*>, MappingImpl>()
    private val mappings = mutableListOf<MappingImpl>()

    init {
        mappingClasses.forEach { (n, c) ->
            if (!c.isData) {
                throw ConfigException("Config mapping class should be data class: $c")
            }
            val mapping = MappingImpl(n, c)
            if (byName.putIfAbsent(n, mapping) != null) {
                throw IllegalArgumentException("Config name conflict: $n")
            }
            if (byClass.putIfAbsent(c, mapping) != null) {
                throw IllegalArgumentException("Config class conflict: $c")
            }
            mappings.add(mapping)
        }
        load()
    }

    override fun <A : Any> get(cls: KClass<A>): A {
        val mapping = byClass[cls]
                ?: throw ConfigException("Config for class doesn't exist: $cls")
        @Suppress("UNCHECKED_CAST")
        return mapping.receiver as A
    }

    override fun get(name: String): Any {
        val mapping = byName[name]
                ?: throw ConfigException("Config doesn't exist: $name")
        return mapping.receiver
    }

    override fun mappings(): List<ConfigMapper.Mapping> {
        return mappings
    }

    override fun reload() {
        config.reload()
        load()
    }

    private fun load() {
        mappings.forEach { mapping ->
            val fragment = config.getFragment(mapping.path)
            mapping.receiver = createObject(mapping.receiverClass, fragment)
        }
    }

    private fun createObject(cls: KClass<*>, fragment: ConfigFragment): Any {
        val constructor = cls.primaryConstructor
                ?: throw ConfigException("Config class should have a primary constructor: $cls")
        val args = Array(constructor.parameters.size) { i ->
            val p = constructor.parameters[i]
            val name = p.name
                    ?: throw ConfigException("Config class parameter has no name: $p")
            val pc = p.type.classifier as? KClass<*>
                    ?: throw ConfigException("Config class parameter has unsupported type: $p")
            if (p.type.isMarkedNullable) {
                throw ConfigException("Config class parameter should not be nullable: $p")
            }
            if (pc.isData) {
                createObject(pc, fragment.getFragment(name))
            } else {
                val value = fragment.get(name)
                when (pc) {
                    String::class -> value.asString()
                    Int::class -> value.asInt()
                    Long::class -> value.asLong()
                    Boolean::class -> value.asBoolean()
                    Double::class -> value.asDouble()
                    else -> throw ConfigException("Config class parameter has unsupported type: $p")
                }
            }
        }
        return constructor.call(*args)
    }

    private class MappingImpl(
            override val path: String,
            override val receiverClass: KClass<*>
    ) : ConfigMapper.Mapping {
        override var receiver: Any = Unit

        override fun toString(): String {
            return "Mapping($path, $receiver)"
        }
    }

    class Builder : ConfigMapper.Builder {
        private var config: Config = EmptyConfig
        private val mappings = mutableMapOf<String, KClass<*>>()

        override fun config(config: Config): ConfigMapper.Builder {
            this.config = config
            return this
        }

        override fun addMapping(name: String, cls: KClass<*>): ConfigMapper.Builder {
            mappings[name] = cls
            return this
        }

        override fun addMapping(cls: KClass<*>): ConfigMapper.Builder {
            val a = cls.findAnnotation<Configuration>()
                    ?: throw ConfigException("Config mapping class should be annotated with @Configuration: $cls")
            return addMapping(a.value, cls)
        }

        override fun build(): ConfigMapper {
            return ConfigMapperImpl(config, mappings)
        }
    }
}