@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.github.niuhf0452.exile.inject

import com.github.niuhf0452.exile.inject.binder.AutowireBinder
import com.github.niuhf0452.exile.inject.impl.InjectorImpl
import kotlin.reflect.KClass

interface InjectContext {
    /**
     * Get the bindings matched with the type.
     *
     * @param key A key of type to inject.
     * @return The bindings matched with the type.
     * @since 1.0
     */
    fun getBindings(key: TypeKey): Injector.BindingSet
}

/**
 * Injector is the container of the dependency injection, it provides the API
 * to add new bindings into the container, refresh existing bindings,
 * and create instance of injected interfaces.
 *
 * @since 1.0
 */
interface Injector : InjectContext, AutoCloseable {
    /**
     * Get all prepared bindings. Since the Injector could run in LAZY mode, this method might not return
     * all bindings.
     * This method is for debugging only.
     *
     * @since 1.0
     */
    fun preparedBindings(): List<Binding>

    /**
     * A collection of bindings of a certain type.
     *
     * @since 1.0
     */
    interface BindingSet : Iterable<Binding> {
        /**
         * This is a convenient method for select a single binding from [BindingSet] by matching qualifiers.
         *
         * @param qualifiers The qualifiers to match with bindings.
         * @return A binding who has all the qualifiers.
         * @throws IllegalArgumentException No bindings or more than one bindings.
         * @since 1.0
         */
        @Throws(IllegalArgumentException::class)
        fun getSingle(qualifiers: List<Annotation> = emptyList()): Binding

        /**
         * This is a convenient method for select a list of bindings from [BindingSet] by matching qualifiers.
         *
         * @param qualifiers The qualifiers to match with bindings.
         * @return A list of bindings who has all the qualifiers.
         * @since 1.0
         */
        fun getList(qualifiers: List<Annotation> = emptyList()): List<Binding>
    }

    /**
     * Binding is the mapping from a type to an implementation class, instance or provider.
     * Binding is also the factory API for creating injected instances.
     *
     * @since 1.0
     */
    interface Binding {
        /**
         * Key is the type of the binding, a.k.a requested type.
         * It might be an interface type.
         */
        val key: TypeKey

        /**
         * Qualifiers of binding.
         * Note that, the qualifiers of binding might be different from the qualifier of implementation class.
         * It's because the binding is created by [Binder] which is flexible, but may not consistent
         * with implementation class.
         */
        val qualifiers: List<Annotation>

        /**
         * Get the instance of injected type.
         * Note that the type of instance could be subclass of [key].
         */
        fun getInstance(): Any
    }

    /**
     * Binder is the source of bindings. It's responsible for creating bindings.
     * Binder is SPI for extending new injection strategy.
     *
     * @since 1.0
     */
    interface Binder {
        /**
         * This method is called by injector internally to collect all bindings.
         * Generally, the injector is lazy, that means it won't collect bindings until some one asked for injection.
         * Then it will call the bind method to prepare for bindings.
         * But also injector has eager mode, that means prepare for bindings at startup.
         */
        fun bind(key: TypeKey, context: BindingContext)
    }

    /**
     * BindingContext is used inside [Binder.bind] to add bindings. It's created by [Injector] internally.
     * It should never be cached or leak to outside of [Binder.bind].
     *
     * @since 1.0
     */
    interface BindingContext : InjectContext {
        fun bindToProvider(qualifiers: List<Annotation>, provider: Provider)

        fun bindToInstance(qualifiers: List<Annotation>, instance: Any)

        fun bindToType(qualifiers: List<Annotation>, implType: TypeKey)
    }

    interface Provider {
        fun getInstance(): Any
    }

    /**
     * A filter of bindings to intercept injected instances,
     * it's generally use for caching, proxying, and access control.
     * An implementation of Filter should respect the qualifiers of the binding.
     * For example, for caching instance, a filter should respect to @Singleton,
     * that means any bindings with @Singleton should be cached, and those
     * without @Singleton should not be cached.
     *
     * @since 1.0
     */
    interface Filter {
        val order: Int

        fun filter(binding: Binding): Binding
    }

    companion object {
        /**
         * Get a builder for building instance of [Injector].
         */
        fun builder(): InjectorBuilder {
            return InjectorImpl.Builder()
        }
    }
}

/**
 * The Inject annotation has multiple functions:
 *
 * 1. Annotate on implementation class of a injectable interface to hint that the class should be discovered
 *    automatically by [AutowireBinder].
 * 2. Annotate on constructor to hint that the constructor should be used to instantiate class by [Injector].
 *
 * @since 1.0
 */
@MustBeDocumented
@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Inject

/**
 * Excludes annotation is used to hint the [AutowireBinder] not wire the decorated class for certain interfaces.
 *
 * @param value Specify the interface classes NOT wired by [AutowireBinder].
 * @since 1.0
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Excludes(vararg val value: KClass<*>)

/**
 * Factory provides support of Spring style configuration.
 *
 * @since 1.0
 */
@Inject
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Factory

interface Provider<A> {
    fun get(): A
}
