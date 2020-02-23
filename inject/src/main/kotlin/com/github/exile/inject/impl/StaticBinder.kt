package com.github.exile.inject.impl

import com.github.exile.inject.InjectorBuilder
import com.github.exile.inject.Injector
import com.github.exile.inject.TypeKey
import com.github.exile.inject.impl.Bindings.InstanceBinding
import com.github.exile.inject.impl.Bindings.ListBindingSet
import com.github.exile.inject.impl.Bindings.ProviderBinding
import com.github.exile.inject.impl.Bindings.SingletonBinding
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType

class StaticBinder(config: (InjectorBuilder.Configurator) -> Unit) : Injector.Binder {
    private val properties = ConfiguratorImpl().run(config)

    override fun bind(key: TypeKey, context: Injector.BindingContext): Injector.BindingSet {
        val ps = properties[key]
                ?: return context.emptyBindingSet()
        return ListBindingSet(ps.map {
            it.createBinding(context)
        })
    }

    private class ConfiguratorImpl : InjectorBuilder.Configurator {
        private val propertyMap = mutableMapOf<TypeKey, MutableList<BindingProperty>>()

        override fun bind(type: KType): InjectorBuilder.BindingBuilder {
            val key = TypeKey(type)
            val properties = propertyMap.computeIfAbsent(key) { mutableListOf() }
            return BindingBuilderImpl(key, properties)
        }

        override fun bind(cls: KClass<*>): InjectorBuilder.BindingBuilder {
            if (cls.typeParameters.isNotEmpty()) {
                throw IllegalStateException("Can't bind generic class: $cls")
            }
            return bind(cls.starProjectedType)
        }

        fun run(configure: (InjectorBuilder.Configurator) -> Unit): Map<TypeKey, List<BindingProperty>> {
            configure(this)
            return propertyMap
        }
    }

    private class BindingBuilderImpl(
            private val key: TypeKey,
            private val properties: MutableList<BindingProperty>
    ) : InjectorBuilder.BindingBuilder {
        override fun toClass(cls: KClass<*>,
                             arguments: List<TypeKey>,
                             qualifiers: List<Annotation>,
                             singleton: Boolean) {
            toType(TypeKey(cls, arguments), qualifiers, singleton)
        }

        override fun toType(type: KType,
                            qualifiers: List<Annotation>,
                            singleton: Boolean) {
            toType(TypeKey(type), qualifiers, singleton)
        }

        private fun toType(targetKey: TypeKey,
                           qualifiers: List<Annotation>,
                           singleton: Boolean) {
            if (targetKey.classifier == key.classifier) {
                throw IllegalArgumentException("Don't bind class to itself: $targetKey")
            }
            if (!key.isAssignableFrom(targetKey)) {
                throw IllegalArgumentException("The implementation type doesn't comply to the bind type: " +
                        "bind type = $key, implementation type = $targetKey")
            }
            properties.add(TypeBindingProperty(key, targetKey, qualifiers, singleton))
        }

        override fun toInstance(instance: Any, qualifiers: List<Annotation>) {
            if (!key.classifier.isInstance(instance)) {
                throw IllegalArgumentException("The type of instance doesn't comply to the bind type: " +
                        "bind type = $key, instance type = ${instance::class}")
            }
            properties.add(InstanceBindingProperty(key, qualifiers, instance))
        }

        override fun toProvider(qualifiers: List<Annotation>, singleton: Boolean, provider: () -> Any) {
            properties.add(ProviderBindingProperty(key, qualifiers, provider, singleton))
        }
    }

    private interface BindingProperty {
        fun createBinding(context: Injector.BindingContext): Injector.Binding
    }

    private class TypeBindingProperty(
            private val key: TypeKey,
            private val targetKey: TypeKey,
            private val qualifiers: List<Annotation>,
            private val singleton: Boolean
    ) : BindingProperty {
        override fun createBinding(context: Injector.BindingContext): Injector.Binding {
            val bindings = context.getDependency(targetKey)
            val iterator = bindings.iterator()
            if (!iterator.hasNext()) {
                throw IllegalStateException("No binding to implementation class, make sure " +
                        "${InstantiateBinder::class} is enabled: $targetKey")
            }
            val binding = iterator.next()
            if (iterator.hasNext()) {
                throw IllegalStateException("More than one binding found for implementation class: $targetKey")
            }
            return ProviderBinding(key, qualifiers) {
                binding.getInstance()
            }.let { b ->
                if (singleton) {
                    SingletonBinding(b)
                } else {
                    b
                }
            }
        }
    }

    private class InstanceBindingProperty(
            private val key: TypeKey,
            private val qualifiers: List<Annotation>,
            private val instance: Any
    ) : BindingProperty {
        override fun createBinding(context: Injector.BindingContext): Injector.Binding {
            return InstanceBinding(key, qualifiers, instance)
        }
    }

    private class ProviderBindingProperty(
            private val key: TypeKey,
            private val qualifiers: List<Annotation>,
            private val provider: () -> Any,
            private val singleton: Boolean
    ) : BindingProperty {
        override fun createBinding(context: Injector.BindingContext): Injector.Binding {
            return ProviderBinding(key, qualifiers, provider).let { b ->
                if (singleton) {
                    SingletonBinding(b)
                } else {
                    b
                }
            }
        }
    }
}