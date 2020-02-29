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
    private fun makeProvider(constructor: KFunction<Any>, key: TypeKey, context: Injector.BindingContext): Injector.Provider {
        val pbs = constructor.parameters.map { p ->
            context.getBindings(resolveType(p.type, key))
                    .getSingle(p.getQualifiers())
        }
        return ConstructorProvider(constructor, pbs)
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
            private val pbs: List<Injector.Binding>
    ) : Injector.Provider {
        override fun getInstance(): Any {
            return if (pbs.isEmpty()) {
                constructor.call()
            } else {
                val params = Array(pbs.size) { i ->
                    pbs[i].getInstance()
                }
                constructor.call(*params)
            }
        }

        override fun toString(): String {
            return "Constructor($constructor)"
        }
    }
}