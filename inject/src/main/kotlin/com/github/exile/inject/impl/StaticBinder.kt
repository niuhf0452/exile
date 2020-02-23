package com.github.exile.inject.impl

import com.github.exile.inject.Injector
import com.github.exile.inject.InjectorBuilder
import com.github.exile.inject.TypeKey
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType

class StaticBinder(config: (InjectorBuilder.Configurator) -> Unit) : Injector.Binder {
    private val binderCallMap = ConfiguratorImpl().run(config)

    override fun bind(key: TypeKey, context: Injector.BindingContext) {
        binderCallMap[key]?.forEach { c -> c(context) }
    }

    private class ConfiguratorImpl : InjectorBuilder.Configurator {
        private val binderCalls = mutableMapOf<TypeKey, MutableList<(Injector.BindingContext) -> Unit>>()

        override fun bind(type: KType): InjectorBuilder.BindingBuilder {
            val key = TypeKey(type)
            val calls = binderCalls.computeIfAbsent(key) { mutableListOf() }
            return BindingBuilderImpl(key, calls)
        }

        override fun bind(cls: KClass<*>): InjectorBuilder.BindingBuilder {
            if (cls.typeParameters.isNotEmpty()) {
                throw IllegalStateException("Can't bind generic class: $cls")
            }
            return bind(cls.starProjectedType)
        }

        fun run(configure: (InjectorBuilder.Configurator) -> Unit): Map<TypeKey, List<(Injector.BindingContext) -> Unit>> {
            configure(this)
            return binderCalls
        }
    }

    private class BindingBuilderImpl(
            private val key: TypeKey,
            private val calls: MutableList<(Injector.BindingContext) -> Unit>
    ) : InjectorBuilder.BindingBuilder {
        override fun toType(type: TypeKey, qualifiers: List<Annotation>) {
            if (type.classifier == key.classifier) {
                throw IllegalArgumentException("Don't bind class to itself: $type")
            }
            if (!key.isAssignableFrom(type)) {
                throw IllegalArgumentException("The implementation type doesn't comply to the bind type: " +
                        "bind type = $key, implementation type = $type")
            }
            calls.add { c -> c.bindToType(key, qualifiers, type) }
        }

        override fun toInstance(instance: Any, qualifiers: List<Annotation>) {
            if (!key.classifier.isInstance(instance)) {
                throw IllegalArgumentException("The type of instance doesn't comply to the bind type: " +
                        "bind type = $key, instance type = ${instance::class}")
            }
            calls.add { c -> c.bindToInstance(key, qualifiers, instance) }
        }

        override fun toProvider(qualifiers: List<Annotation>, provider: () -> Any) {
            calls.add { c -> c.bindToProvider(key, qualifiers, provider) }
        }
    }
}