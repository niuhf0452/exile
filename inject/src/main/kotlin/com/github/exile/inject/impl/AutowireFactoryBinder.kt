package com.github.exile.inject.impl

import com.github.exile.inject.Factory
import com.github.exile.inject.Injector
import com.github.exile.inject.TypeKey
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation

class AutowireFactoryBinder : Injector.Binder {
    private var initialized = false
    private val providers = mutableMapOf<TypeKey, MethodProvider>()

    override fun bind(key: TypeKey, context: Injector.BindingContext) {
        initialize(context)
        val p = providers[key]
        if (p != null) {
            context.bindToProvider(p.function.getQualifiers(), p)
        }
    }

    private fun initialize(context: Injector.BindingContext) {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    // avoid re-entering
                    initialized = true
                    loadAllFactories(context)
                }
            }
        }
    }

    private fun loadAllFactories(context: Injector.BindingContext) {
        val scanner = context.getBindings(TypeKey(Injector.Scanner::class))
                .getSingle().getInstance() as Injector.Scanner
        scanner.findByAnnotation(Factory::class).forEach { factoryCls ->
            if (factoryCls.isAbstract) {
                throw IllegalStateException("@Factory class doesn't support abstract type: $factoryCls")
            }
            if (factoryCls.typeParameters.isNotEmpty()) {
                throw IllegalStateException("@Factory class doesn't support generic type: $factoryCls")
            }
            factoryCls.declaredFunctions.forEach { f ->
                if (f.findAnnotation<Factory>() != null) {
                    if (f.typeParameters.isNotEmpty()) {
                        throw IllegalStateException("@Factory method doesn't support type parameter: $f")
                    }
                    providers[TypeKey(f.returnType)] = makeProvider(f, context)
                }
            }
        }
    }

    private fun makeProvider(function: KFunction<*>, context: Injector.BindingContext): MethodProvider {
        val params = function.parameters.map { p ->
            val bindings = context.getBindings(TypeKey(p.type)).getList(p.getQualifiers())
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
        return MethodProvider(function, params)
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

    private class MethodProvider(
            val function: KFunction<*>,
            private val params: List<() -> Any?>
    ) : Injector.Provider {
        override fun getInstance(): Any {
            val value = if (params.isEmpty()) {
                function.call()
            } else {
                val args = Array(params.size) { i ->
                    params[i]()
                }
                function.call(*args)
            }
            return value
                    ?: throw IllegalArgumentException("Factory method returned a null value: $function")
        }

        override fun toString(): String {
            return "Method($function)"
        }
    }
}