package com.github.exile.inject.impl

import com.github.exile.inject.Inject
import com.github.exile.inject.Injector
import com.github.exile.inject.TypeKey
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

class InstantiateBinder : Injector.Binder {
    override fun bind(key: TypeKey, context: Injector.BindingContext) {
        val cls = key.classifier
        if (!cls.isAbstract) {
            val instance = cls.objectInstance
            if (instance != null) {
                context.bindToInstance(key, cls.getQualifiers(), instance)
            } else {
                val constructor = findConstructor(cls)
                if (constructor != null) {
                    val provider = makeProvider(constructor, key, context)
                    context.bindToProvider(key, cls.getQualifiers(), provider)
                }
            }
        }
    }

    /**
     * Create a instance factory which calls the constructor and inject parameters.
     */
    private fun makeProvider(constructor: KFunction<Any>,
                             key: TypeKey,
                             context: Injector.BindingContext): Injector.Provider {
        val params = constructor.parameters.map { p ->
            val bindings = context.getBindings(resolveType(p.type, key)).getList(p.getQualifiers())
            when (bindings.size) {
                0 -> {
                    if (!p.type.isMarkedNullable) {
                        throw IllegalStateException("No binding for parameter: $p")
                    }
                    NullProvider
                }
                1 -> BindingProvider(bindings[0])
                else -> throw IllegalStateException("Multiple bindings for parameter: $p")
            }
        }
        return ConstructorProvider(constructor, params)
    }

    private fun findConstructor(cls: KClass<*>): KFunction<Any>? {
        cls.constructors.forEach { c ->
            if (c.findAnnotation<Inject>() != null) {
                return c
            }
        }
        if (cls.findAnnotation<Inject>() != null) {
            return cls.primaryConstructor
        }
        return null
    }

    private fun resolveType(t: KType, key: TypeKey): TypeKey {
        val p = t.classifier as? KTypeParameter
                ?: return TypeKey(t)
        val i = key.classifier.typeParameters.indexOf(p)
        return key.arguments[i]
    }

    private class ConstructorProvider(
            private val constructor: KFunction<Any>,
            private val params: List<() -> Any?>
    ) : Injector.Provider {
        override fun getInstance(): Any {
            return if (params.isEmpty()) {
                constructor.call()
            } else {
                val params = Array(params.size) { i ->
                    params[i]()
                }
                constructor.call(*params)
            }
        }

        override fun toString(): String {
            return "Constructor($constructor)"
        }
    }

    private object NullProvider : () -> Any? {
        override fun invoke(): Any? {
            return null
        }
    }

    private class BindingProvider(
            private val binding: Injector.Binding
    ) : () -> Any? {
        override fun invoke(): Any? {
            return binding.getInstance()
        }
    }
}