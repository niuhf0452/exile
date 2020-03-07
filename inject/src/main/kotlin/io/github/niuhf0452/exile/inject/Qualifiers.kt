package io.github.niuhf0452.exile.inject

import io.github.niuhf0452.exile.inject.impl.SingletonScope
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

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
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION)
annotation class Named(val value: String)

/**
 * The ScopeQualifier is to annotate on a qualifier to hint the injected object should be cached in
 * a certain scope.
 *
 * @see Singleton
 * @since 1.0
 */
@MustBeDocumented
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ScopeQualifier(val value: KClass<out Scope<*>>)

/**
 * The Singleton annotation is a hint to injector that make it caches the instance injected,
 * so that the further injecting will reuse the cached instance.
 *
 * @since 1.0
 */
@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@ScopeQualifier(SingletonScope::class)
annotation class Singleton

/**
 * Scope is responsible for container aware.
 *
 * In a scoped injection, instances are cached in the containers, each container is stand for a scope instance.
 * Injector use Scope to find the container for the running context.
 *
 * So the method [getContainer] will be called for every injecting.
 *
 * @see Singleton
 * @since 1.0
 */
interface Scope<A : Annotation> {
    fun getContainer(): Container<A>

    interface Container<A> {
        fun getOrCreate(id: String, qualifier: A, provider: () -> Any): Any
    }
}
