package io.github.niuhf0452.exile.inject

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVariance
import kotlin.reflect.full.allSupertypes

data class TypeKey(val classifier: KClass<*>, val arguments: List<TypeKey>) {
    init {
        if (classifier.typeParameters.size != arguments.size) {
            throw IllegalArgumentException("Invalid number of type arguments: $classifier")
        }
    }

    override fun toString(): String {
        if (arguments.isEmpty()) {
            return classifier.toString()
        }
        return arguments.joinToString(prefix = "$classifier<", postfix = ">", separator = ", ")
    }

    fun isAssignableFrom(key: TypeKey): Boolean {
        if (classifier == key.classifier) {
            return arguments == key.arguments
        }
        val t = key.classifier.allSupertypes.find { it.classifier == classifier }
                ?: return false
        t.arguments.forEachIndexed { index, (variance, type) ->
            if (variance == null || type == null || variance != KVariance.INVARIANT) {
                return false
            }
            when (val c = type.classifier) {
                is KClass<*> -> {
                    if (TypeKey(type) != arguments[index]) {
                        return false
                    }
                }
                is KTypeParameter -> {
                    val i = key.classifier.typeParameters.indexOf(c)
                    if (key.arguments[i] != arguments[index]) {
                        return false
                    }
                }
                else -> return false
            }
        }
        return true
    }

    companion object {
        operator fun invoke(type: KType): TypeKey {
            when (val c = type.classifier) {
                is KClass<*> -> {
                    val args = type.arguments.map { (variance, t) ->
                        if (variance == null || t == null) {
                            throw IllegalArgumentException("Not support type with star parameter: $type")
                        }
                        if (variance != KVariance.INVARIANT) {
                            throw IllegalArgumentException("Not support type with variant parameter: $type")
                        }
                        invoke(t)
                    }
                    return TypeKey(c, args)
                }
                null -> throw IllegalArgumentException("Not support intersection type: $type")
                is KTypeParameter -> throw IllegalArgumentException("Not support parameter type: $type")
                else -> throw IllegalArgumentException("Not support this type: $type")
            }
        }

        operator fun invoke(cls: KClass<*>): TypeKey {
            return TypeKey(cls, emptyList())
        }
    }
}