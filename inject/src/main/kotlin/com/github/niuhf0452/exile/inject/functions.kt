@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.github.niuhf0452.exile.inject

import kotlin.reflect.KClass

/**
 * Inject a single instance of class `cls`.
 * The binding is selected by matching the qualifier, one and only one binding is selected.
 * An exception is thrown if can't select exactly one binding.
 *
 * @param key The type to inject.
 * @param qualifiers The qualifiers The qualifiers to match with bindings.
 * @return The instance of class `cls`.
 * @since 1.0
 */
fun <A : Any> InjectContext.getInstance(key: TypeKey, qualifiers: List<Annotation> = emptyList()): A {
    val bindings = getBindings(key)
    val binding = try {
        bindings.getSingle(qualifiers)
    } catch (ex: InjectException) {
        throw InjectException("Can't find suitable binding for type: $key", ex)
    }
    @Suppress("UNCHECKED_CAST")
    return binding.getInstance() as A
}

fun <A : Any> InjectContext.getInstance(cls: KClass<A>, qualifiers: List<Annotation> = emptyList()): A {
    return getInstance(TypeKey(cls), qualifiers)
}

fun <A> InjectContext.getProvider(key: TypeKey, qualifiers: List<Annotation> = emptyList()): Provider<A> {
    return getInstance(key, qualifiers)
}

fun <A : Any> InjectContext.getProvider(cls: KClass<A>, qualifiers: List<Annotation> = emptyList()): Provider<A> {
    return getProvider(TypeKey(Provider::class, listOf(TypeKey(cls))), qualifiers)
}