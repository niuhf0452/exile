package com.github.exile.inject

import com.github.exile.inject.impl.*
import kotlin.reflect.KClass
import kotlin.reflect.KType

class InjectorBuilder {
    private val binders = mutableListOf<Injector.Binder>()

    fun addBinder(binder: Injector.Binder): InjectorBuilder {
        binders.add(binder)
        return this
    }

    fun enableAutowire(packageNames: List<String>): InjectorBuilder {
        return addBinder(AutowireBinder(ClassgraphScanner(packageNames)))
    }

    fun enableStatic(config: (Configurator) -> Unit): InjectorBuilder {
        return addBinder(StaticBinder(config))
    }

    fun enableServiceLoader(): InjectorBuilder {
        return addBinder(ServiceLoaderBinder())
    }

    fun build(): Injector {
        binders.add(InstantiateBinder())
        return InjectorImpl(binders)
    }

    interface Configurator {
        fun bind(type: KType): BindingBuilder

        fun bind(cls: KClass<*>): BindingBuilder
    }

    interface BindingBuilder {
        fun toClass(cls: KClass<*>,
                    arguments: List<TypeKey> = emptyList(),
                    qualifiers: List<Annotation> = emptyList(),
                    singleton: Boolean = false)

        fun toType(type: KType,
                   qualifiers: List<Annotation> = emptyList(),
                   singleton: Boolean = false)

        fun toInstance(instance: Any, qualifiers: List<Annotation> = emptyList())

        fun toProvider(qualifiers: List<Annotation> = emptyList(),
                       singleton: Boolean = false,
                       provider: () -> Any)
    }
}