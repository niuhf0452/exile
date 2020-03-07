package io.github.niuhf0452.exile.inject

import kotlin.reflect.KClass

/**
 * ClassScanner is SPI used to extend the class scan behaviour.
 *
 * @since 1.0
 */
interface ClassScanner {
    fun findBySuperClass(cls: KClass<*>): Iterable<KClass<*>>

    fun findByAnnotation(cls: KClass<out Annotation>): Iterable<KClass<*>>

    interface Factory {
        fun createScanner(packageNames: List<String>): ClassScanner
    }
}