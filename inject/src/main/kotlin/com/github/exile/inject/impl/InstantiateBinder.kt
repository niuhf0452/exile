package com.github.exile.inject.impl

import com.github.exile.inject.Inject
import com.github.exile.inject.Injector
import com.github.exile.inject.TypeKey
import com.github.exile.inject.getSingle
import com.github.exile.inject.impl.Bindings.InstanceBinding
import com.github.exile.inject.impl.Bindings.ListBindingSet
import com.github.exile.inject.impl.Bindings.ProviderBinding
import com.github.exile.inject.impl.Bindings.getQualifiers
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

class InstantiateBinder : Injector.Binder {
    override fun bind(key: TypeKey, context: Injector.BindingContext): Injector.BindingSet {
        val cls = key.classifier
        if (cls.isAbstract) {
            return context.emptyBindingSet()
        }
        cls.objectInstance?.let { instance ->
            return ListBindingSet(listOf(InstanceBinding(key, cls.getQualifiers(), instance)))
        }
        val constructor = findConstructor(cls)
                ?: return context.emptyBindingSet()
        if (constructor.parameters.isEmpty()) {
            return ListBindingSet(listOf(ProviderBinding(key, cls.getQualifiers()) {
                constructor.call()
            }))
        }
        val pbs = constructor.parameters.map { p ->
            context.getDependency(resolveType(p.type, key))
                    .getSingle(p.getQualifiers())
        }
        return ListBindingSet(listOf(ProviderBinding(key, cls.getQualifiers()) {
            val params = Array(pbs.size) { i ->
                pbs[i].getInstance()
            }
            constructor.call(*params)
        }))
    }

    private fun findConstructor(cls: KClass<*>): KFunction<Any>? {
        if (cls.findAnnotation<Inject>() != null) {
            return cls.primaryConstructor
        }
        cls.constructors.forEach { c ->
            if (c.findAnnotation<Inject>() != null) {
                return c
            }
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