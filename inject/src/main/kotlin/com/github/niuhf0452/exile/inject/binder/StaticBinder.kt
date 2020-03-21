package com.github.niuhf0452.exile.inject.binder

import com.github.niuhf0452.exile.inject.Injector
import com.github.niuhf0452.exile.inject.InjectorBuilder
import com.github.niuhf0452.exile.inject.TypeKey
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
                throw IllegalArgumentException("Can't bind generic class: $cls")
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
            calls.add { c -> c.bindToType(qualifiers, type) }
        }

        override fun toInstance(instance: Any, qualifiers: List<Annotation>) {
            if (!key.classifier.isInstance(instance)) {
                throw IllegalArgumentException("The type of instance doesn't comply to the bind type: " +
                        "bind type = $key, instance type = ${instance::class}")
            }
            calls.add { c -> c.bindToInstance(qualifiers, instance) }
        }

        override fun toProvider(qualifiers: List<Annotation>, provider: () -> Any) {
            toProvider(qualifiers, ProviderAdapter(provider))
        }

        override fun toProvider(qualifiers: List<Annotation>, provider: Injector.Provider) {
            calls.add { c -> c.bindToProvider(qualifiers, provider) }
        }
    }

    private class ProviderAdapter(
            private val provider: () -> Any
    ) : Injector.Provider {
        override fun getInstance(): Any {
            return provider()
        }

        override fun toString(): String {
            return "ProviderAdapter($provider)"
        }
    }
}