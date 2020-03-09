package com.github.niuhf0452.exile.inject

import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVariance


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
                throw IllegalArgumentException("Don't mixin TypeLiteral.")
            }
            if (arr[0].classifier != TypeLiteral::class) {
                throw IllegalArgumentException("TypeLiteral could only be inherited directly.")
            }
            val (variance, type) = arr[0].arguments.first()
            if (variance == null || type == null) {
                throw IllegalArgumentException("Don't use star with TypeLiteral.")
            }
            if (variance != KVariance.INVARIANT) {
                throw IllegalArgumentException("Don't use variiant with TypeLiteral.")
            }
            if (type.classifier is KTypeParameter) {
                throw IllegalArgumentException("Don't use parameter type with TypeLiteral.")
            }
            return type
        }

    val typeKey: TypeKey
        get() = TypeKey(type)
}