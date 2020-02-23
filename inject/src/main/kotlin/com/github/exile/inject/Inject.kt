@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.github.exile.inject

import com.github.exile.inject.impl.AutowireBinder
import com.github.exile.inject.impl.Injectors
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVariance
import kotlin.reflect.full.primaryConstructor

/**
 * Injector is the container of the dependency injection, it provides the API
 * to add new bindings into the container, refresh existing bindings,
 * and create instance of injected interfaces.
 *
 * @since 1.0
 */
interface Injector {
    /**
     * Get the bindings matched with the type.
     *
     * @param key A key of type to inject.
     * @return The bindings matched with the type.
     * @since 1.0
     */
    fun getBindings(key: TypeKey): BindingSet

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
         * @throws IllegalStateException No bindings or more than one bindings.
         * @since 1.0
         */
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
    interface BindingContext {
        fun getBindings(key: TypeKey): BindingSet

        fun bindToProvider(key: TypeKey, qualifiers: List<Annotation>, provider: () -> Any)

        fun bindToInstance(key: TypeKey, qualifiers: List<Annotation>, instance: Any)

        fun bindToType(key: TypeKey, qualifiers: List<Annotation>, implType: TypeKey)
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

    /**
     * Enhancer is adapter interface to AOP components, e.g. CGLib, ByteBuddy.
     *
     * @since 1.0
     */
    interface Enhancer {
        fun enhance(cls: KClass<*>): (List<Any?>) -> Any
    }

    /**
     * ClassScanner is SPI used to extend the class scan behaviour.
     *
     * @since 1.0
     */
    interface Scanner {
        fun findByInterface(cls: KClass<*>): Iterable<KClass<*>>

        fun findByAnnotation(cls: KClass<out Annotation>): Iterable<KClass<*>>
    }

    enum class LoadingMode {
        /**
         * Load all bindings when instantiating [Injector], after that the internal state of injector is immutable,
         * it doesn't lazy load bindings for new type.
         */
        EAGER,
        /**
         * Don't load binding util it's asked for injecting. The internal state of injector is mutable,
         * so that it can always add bindings for new type.
         */
        LAZY,
        /**
         * It works like [LAZY] expect that a thread is started to load bindings when instantiating [Injector].
         */
        ASYNC
    }

    companion object {
        /**
         * Get a builder for building instance of [Injector].
         */
        fun builder(): InjectorBuilder {
            return Injectors.Builder()
        }
    }
}

/**
 * Inject a single instance of class `cls`.
 * The binding is selected by matching the qualifier, one and only one binding is selected.
 * An exception is thrown if can't select exactly one binding.
 *
 * @param cls The class to inject.
 * @param qualifiers The qualifiers The qualifiers to match with bindings.
 * @return The instance of class `cls`.
 * @throws IllegalStateException No bindings or more than one bindings.
 * @since 1.0
 */
@Throws(IllegalStateException::class)
fun <A : Any> Injector.getInstance(cls: KClass<A>, qualifiers: List<Annotation> = emptyList()): A {
    val binding = getBindings(TypeKey(cls)).getSingle(qualifiers)
    @Suppress("UNCHECKED_CAST")
    return binding.getInstance() as A
}

/**
 * The Inject annotation has multiple functions:
 *
 * 1. Annotate on interface to hint that the interface should be loaded in [EAGER][Injector.LoadingMode.EAGER] mode
 *    and [ASYNC][Injector.LoadingMode.ASYNC] mode.
 * 2. Annotate on implementation class of a injectable interface to hint that the class should be discovered
 *    automatically by [AutowireBinder].
 * 3. Annotate on constructor to hint that the constructor should be used to instantiate class by [Injector].
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
 * The Qualifier annotation is used to annotate other annotation classes for hinting the injector
 * that they are qualifiers and should be collected for selecting.
 *
 * @since 1.0
 */
@MustBeDocumented
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Qualifier

/**
 * The Named annotation gives the implementation class a identity name which is used as selector
 * in injecting.
 *
 * For example, if we have two implementations for the CacheStore interface, one is Redis and another is Memcached.
 * We can use qualifier to differentiate the two implementations:
 *
 * ```kotlin
 *
 * interface CacheStore
 *
 * @Autowire
 * @Named("redis")
 * interface RedisStore : CacheStore
 *
 * @Autowire
 * @Named("memcached")
 * interface MemcachedStore : CacheStore
 *
 * class Service(
 *   @Named("memcached")
 *   val store: CacheStore
 * )
 *
 * @Test
 * fun test() {
 *     assert(injector.getInstance(CacheStore::class, "redis") is RedisStore)
 *     assert(injector.getInstance(CacheStore::class, "memcached") is MemcachedStore)
 *     assert(injector.getInstance(Service::class).store is MemcachedStore)
 * }
 * ```
 *
 * @since 1.0
 */
@Qualifier
@MustBeDocumented
@Target(AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Named(val value: String)

/**
 * The Singleton annotation is a hint to injector that make it caches the instance injected,
 * so that the further injecting will reuse the cached instance.
 *
 * @since 1.0
 */
@Qualifier
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Singleton

/**
 * This is a helper class for creating qualifier/annotation instances,
 * since kotlin doesn't support instantiate annotation class with 'new' operator.
 *
 * @since 1.0
 */
object Qualifiers {
    fun <A : Annotation> qualifier(cls: KClass<A>, vararg args: Any?): A {
        return cls.primaryConstructor!!.call(*args)
    }

    fun named(value: String): Named = qualifier(Named::class, value)

    fun singleton(): Singleton = qualifier(Singleton::class)
}

/**
 * A TypeLiteral is used to construct Type instance from literal.
 * To get a Type, write code like this:
 *
 * ```kotlin
 * val type = object: TypeLiteral<String>() {}.type
 * assert(type.classifier == String::class)
 * ```
 *
 * @since 1.0
 */
abstract class TypeLiteral<A> {
    val type: KType
        get() {
            val arr = this::class.supertypes
            if (arr.size != 1) {
                throw IllegalStateException("Don't mixin TypeLiteral.")
            }
            if (arr[0].classifier != TypeLiteral::class) {
                throw IllegalStateException("TypeLiteral could only be inherited directly.")
            }
            val (variance, type) = arr[0].arguments.first()
            if (variance == null || type == null) {
                throw IllegalStateException("Don't use star with TypeLiteral.")
            }
            if (variance != KVariance.INVARIANT) {
                throw IllegalStateException("Don't use variiant with TypeLiteral.")
            }
            if (type.classifier is KTypeParameter) {
                throw IllegalStateException("Don't use parameter type with TypeLiteral.")
            }
            return type
        }

    val typeKey: TypeKey
        get() = TypeKey(type)
}
