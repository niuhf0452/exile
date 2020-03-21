package com.github.niuhf0452.exile.inject

import com.github.niuhf0452.exile.inject.impl.AutoConfigurator
import kotlin.reflect.KClass
import kotlin.reflect.KType

interface InjectorBuilder {
    fun addPackage(packageName: String): InjectorBuilder

    fun scanner(scannerFactory: ClassScanner.Factory): InjectorBuilder

    fun enhancer(enhancer: ClassEnhancer): InjectorBuilder

    fun addInterceptor(interceptor: ClassInterceptor): InjectorBuilder

    fun addBinder(binder: Injector.Binder): InjectorBuilder

    fun addFilter(filter: Injector.Filter): InjectorBuilder

    fun enableAutowire(): InjectorBuilder

    fun enableStatic(config: (Configurator) -> Unit): InjectorBuilder

    fun enableServiceLoader(): InjectorBuilder

    fun enableScope(): InjectorBuilder

    fun build(): Injector

    interface Configurator {
        fun bind(type: KType): BindingBuilder

        fun bind(cls: KClass<*>): BindingBuilder
    }

    interface BindingBuilder {
        fun toType(type: TypeKey, qualifiers: List<Annotation> = emptyList())

        fun toInstance(instance: Any, qualifiers: List<Annotation> = emptyList())

        fun toProvider(qualifiers: List<Annotation> = emptyList(), provider: () -> Any)

        fun toProvider(qualifiers: List<Annotation> = emptyList(), provider: Injector.Provider)
    }
}

fun InjectorBuilder.autoConfigure(vararg packageNames: String): InjectorBuilder {
    return AutoConfigurator(packageNames.toList(), this).configure()
}

/**
 * This interface is used with ServiceLoader to auto config [Injector].
 */
interface InjectorAutoLoader {
    fun getBinders(): List<Injector.Binder> = emptyList()

    fun getFilters(): List<Injector.Filter> = emptyList()
}