package com.github.exile.inject

import kotlin.reflect.KClass
import kotlin.reflect.KType

interface InjectorBuilder {
    fun scanner(scanner: Injector.Scanner): InjectorBuilder

    fun addBinder(binder: Injector.Binder): InjectorBuilder

    fun loadingMode(mode: Injector.LoadingMode): InjectorBuilder

    fun enableAutowire(): InjectorBuilder

    fun enableStatic(config: (Configurator) -> Unit): InjectorBuilder

    fun enableServiceLoader(): InjectorBuilder

    fun build(): Injector

    interface Configurator {
        fun bind(type: KType): BindingBuilder

        fun bind(cls: KClass<*>): BindingBuilder
    }

    interface BindingBuilder {
        fun toType(type: TypeKey, qualifiers: List<Annotation> = emptyList())

        fun toInstance(instance: Any, qualifiers: List<Annotation> = emptyList())

        fun toProvider(qualifiers: List<Annotation> = emptyList(), provider: () -> Any)
    }
}
