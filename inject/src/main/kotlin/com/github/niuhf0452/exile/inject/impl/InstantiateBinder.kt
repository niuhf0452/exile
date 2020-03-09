package com.github.niuhf0452.exile.inject.impl

import com.github.niuhf0452.exile.inject.*
import java.lang.reflect.Modifier
import kotlin.reflect.*
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaMethod

class InstantiateBinder(
        private val packageNames: List<String>,
        private val enhancer: ClassEnhancer,
        private val interceptors: List<ClassInterceptor>
) : Injector.Binder {
    override fun bind(key: TypeKey, context: Injector.BindingContext) {
        val cls = key.classifier
        if (!cls.isAbstract) {
            val instance = cls.objectInstance
            if (instance != null) {
                context.bindToInstance(cls.getQualifiers(), instance)
            } else {
                val constructor = findConstructor(cls)
                if (constructor != null) {
                    val enhanced = enhanceClass(cls)
                    val constructor1 = findConstructor(enhanced)
                            ?: throw IllegalStateException()
                    val provider = makeProvider(constructor1, key, context)
                    context.bindToProvider(cls.getQualifiers(), provider)
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
                else -> throw IllegalStateException(bindings.joinToString(
                        "\n- ", "Multiple bindings for parameter: $p\n- "))
            }
        }
        return ConstructorProvider(constructor, params)
    }

    private fun findConstructor(cls: KClass<*>): KFunction<Any>? {
        cls.constructors.forEach { c ->
            if (isInjectable(c)) {
                return c
            }
        }
        if (isInjectable(cls)) {
            return cls.primaryConstructor
                    ?: cls.constructors.firstOrNull()
        }
        return null
    }

    private fun isInjectable(element: KAnnotatedElement): Boolean {
        if (element.findAnnotation<Inject>() != null) {
            return true
        }
        element.annotations.forEach { a ->
            if (a.annotationClass.findAnnotation<Inject>() != null) {
                return true
            }
        }
        return false
    }

    private fun resolveType(t: KType, key: TypeKey): TypeKey {
        val p = t.classifier as? KTypeParameter
                ?: return TypeKey(t)
        val i = key.classifier.typeParameters.indexOf(p)
        return key.arguments[i]
    }

    private fun enhanceClass(cls: KClass<*>): KClass<*> {
        val methods = mutableListOf<ClassEnhancer.MethodInfo>()
        cls.memberFunctions.forEach { m ->
            if (!isIgnore(m)) {
                val interceptorList = interceptors.mapNotNull { it.intercept(m) }
                if (interceptorList.isNotEmpty()) {
                    checkModifiable(m)
                    methods.add(ClassEnhancer.MethodInfo(m, interceptorList))
                }
            }
        }
        if (methods.isEmpty()) {
            return cls
        }
        return enhancer.enhance(cls, methods)
    }

    private fun isIgnore(m: KFunction<*>): Boolean {
        if (m.findAnnotation<AopIgnore>() != null) {
            return true
        }
        m.annotations.forEach { a ->
            if (a.annotationClass.findAnnotation<AopIgnore>() != null) {
                return true
            }
        }
        val packageName = m.javaMethod?.declaringClass?.packageName
                ?: return true
        packageNames.forEach { pn ->
            if (packageName.startsWith(pn)
                    && (packageName.length == pn.length || packageName[pn.length] == '.')) {
                return false
            }
        }
        return true
    }

    private fun checkModifiable(m: KFunction<*>) {
        val modifiers = m.javaMethod?.modifiers
                ?: throw IllegalArgumentException("Can't find java method: $m")
        if (!Modifier.isProtected(modifiers) && !Modifier.isPublic(modifiers)) {
            throw IllegalArgumentException("Private or package internal method can't be intercepted: $m")
        }
        if (Modifier.isFinal(modifiers)) {
            throw IllegalArgumentException("Final method can't be intercepted: $m")
        }
    }

    private class ConstructorProvider(
            private val constructor: KFunction<Any>,
            private val params: List<() -> Any?>
    ) : Injector.Provider {
        override fun getInstance(): Any {
            return if (params.isEmpty()) {
                constructor.call()
            } else {
                val args = Array(params.size) { i ->
                    params[i]()
                }
                constructor.call(*args)
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