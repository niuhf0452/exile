@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.github.exile.inject

import com.github.exile.inject.Injector.BindingSet
import com.github.exile.inject.impl.AutowireBinder
import com.github.exile.inject.impl.Bindings
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
     */
    fun getBindings(key: TypeKey): BindingSet

    /**
     * A collection of bindings of a certain type.
     */
    interface BindingSet : Iterable<Binding>

    /**
     * Binding is the mapping from a type to an implement class or instance.
     */
    interface Binding {
        val key: TypeKey
        val qualifiers: List<Annotation>

        fun getInstance(): Any
    }

    interface Binder {
        fun bind(key: TypeKey, context: BindingContext): BindingSet
    }

    interface BindingContext {
        fun emptyBindingSet(): BindingSet

        fun getDependency(type: KType): BindingSet

        fun getDependency(key: TypeKey): BindingSet
    }

    companion object {
        fun builder(): InjectorBuilder {
            return InjectorBuilder()
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
 */
@Throws(IllegalStateException::class)
fun <A : Any> Injector.getInstance(cls: KClass<A>, qualifiers: List<Annotation> = emptyList()): A {
    val binding = getBindings(TypeKey(cls)).getSingle(qualifiers)
    @Suppress("UNCHECKED_CAST")
    return binding.getInstance() as A
}

/**
 * This is a convenient method for select a list of bindings from [BindingSet] by matching qualifiers.
 *
 * @param qualifiers The qualifiers to match with bindings.
 * @return A list of bindings who has all the qualifiers.
 */
fun BindingSet.getList(qualifiers: List<Annotation> = emptyList()): List<Injector.Binding> {
    return Bindings.getList(this, qualifiers)
}

/**
 * This is a convenient method for select a single binding from [BindingSet] by matching qualifiers.
 *
 * @param qualifiers The qualifiers to match with bindings.
 * @return A binding who has all the qualifiers.
 * @throws IllegalStateException No bindings or more than one bindings.
 */
fun BindingSet.getSingle(qualifiers: List<Annotation> = emptyList()): Injector.Binding {
    return Bindings.getSingle(this, qualifiers)
}

/**
 * Inject should decorate the implementation class of a injectable interface.
 * It's a hint for injector to find the constructor in injecting.
 * If @Inject decorates the class, then the primary constructor will be used.
 * If @Inject decorates the constructor, then the decorated constructor will be used.
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

object Qualifiers {
    fun <A : Annotation> qualifier(cls: KClass<A>, vararg args: Any?): A {
        return cls.primaryConstructor!!.call(*args)
    }

    fun named(value: String): Named = qualifier(Named::class, value)
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

/**
 * ClassScanner is SPI used to extend the class scan behaviour.
 *
 * @since 1.0
 */
interface ClassScanner {
    fun findByInterface(cls: KClass<*>): Iterable<KClass<*>>
}