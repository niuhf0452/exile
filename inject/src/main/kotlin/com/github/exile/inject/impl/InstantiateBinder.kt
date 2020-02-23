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
                    val factory = makeInstanceFactory(constructor, key, context)
                    context.bindToProvider(key, cls.getQualifiers(), factory)
                }
            }
        }
    }

    /**
     * Create a instance factory which calls the constructor and inject parameters.
     */
    private fun makeInstanceFactory(constructor: KFunction<Any>, key: TypeKey, context: Injector.BindingContext): () -> Any {
        if (constructor.parameters.isEmpty()) {
            return {
                constructor.call()
            }
        }
        val pbs = constructor.parameters.map { p ->
            context.getBindings(resolveType(p.type, key))
                    .getSingle(p.getQualifiers())
        }
        return {
            val params = Array(pbs.size) { i ->
                pbs[i].getInstance()
            }
            constructor.call(*params)
        }
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
}