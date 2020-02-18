package com.github.exile.inject

import com.github.exile.inject.impl.AutowireBinder
import com.github.exile.inject.impl.ClassgraphScanner
import com.github.exile.inject.impl.InjectorImpl
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVariance
import kotlin.reflect.full.starProjectedType

/**
 * Injector is the container of the dependency injection, it provides the API
 * to add new bindings into the container, refresh existing bindings,
 * and create instance of injected interfaces.
 * @since 1.0
 */
interface Injector {
    fun getBinding(type: KType, qualifier: String = ""): Binding

    fun <A : Any> getInstance(cls: KClass<A>, qualifier: String = ""): A {
        @Suppress("UNCHECKED_CAST")
        return getBinding(cls.starProjectedType, qualifier).getInstance() as A
    }

    interface Binding {
        fun getInstance(): Any?
    }

    interface Binder {
        fun getBinding(context: BindingContext, upstream: Binding): Binding
    }

    interface BindingContext {
        val key: Key

        fun getDependency(type: KType, qualifier: String): Binding
    }

    interface Builder {
        fun addBinder(binder: Binder): Builder

        fun build(): Injector
    }

    data class Key(val type: KType, val qualifier: String) {
        override fun toString(): String {
            return if (qualifier.isEmpty()) {
                "type $type"
            } else {
                "type $type with qualifier '$qualifier'"
            }
        }
    }

    class EmptyBinding(
            private val key: Key
    ) : Binding {
        override fun getInstance(): Any? {
            throw IllegalStateException("Failed to inject $key.")
        }
    }

    companion object {
        fun builder(): Builder {
            return InjectorImpl.Builder()
        }

        fun createInjector(): Injector {
            return builder()
                    .addBinder(AutowireBinder())
                    .build()
        }
    }
}

/**
 * Autowire is used to decorate the implementation class of a injectable interface.
 * A Injector can bind the implementation class to the interface automatically,
 * if [AutowireBinder] is enabled.
 *
 * @param value Specify the interface classes bind to.
 *              If it's empty, Injector will search all the interfaces the class implemented.
 * @since 1.0
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Autowire(val value: Array<KClass<*>> = [])

/**
 * Qualifier gives the implementation class a identity name which is used as selector
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
 * @Qualifier("redis")
 * interface RedisStore : CacheStore
 *
 * @Autowire
 * @Qualifier("memcached")
 * interface MemcachedStore : CacheStore
 *
 * @Test
 * fun test() {
 *     assert(injector.getInstance(CacheStore::class, "redis") is RedisStore)
 *     assert(injector.getInstance(CacheStore::class, "memcached") is MemcachedStore)
 * }
 * ```
 *
 * @since 1.0
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Qualifier(val value: String)

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
}

/**
 * ClassScanner is SPI used to extend the class scan behaviour.
 *
 * @since 1.0
 */
interface ClassScanner {
    fun findByInterface(cls: KClass<*>): Iterable<KClass<*>>

    companion object {
        fun classgraphScanner(packageNames: List<String>): ClassScanner {
            return ClassgraphScanner(packageNames)
        }
    }
}