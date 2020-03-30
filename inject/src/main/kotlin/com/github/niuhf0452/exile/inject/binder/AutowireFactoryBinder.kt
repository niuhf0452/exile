package com.github.niuhf0452.exile.inject.binder

import com.github.niuhf0452.exile.inject.*
import com.github.niuhf0452.exile.inject.internal.getQualifiers
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation

class AutowireFactoryBinder : Injector.Binder {
    private var initialized = false
    private val providers = mutableMapOf<TypeKey, MethodProvider>()

    override fun bind(key: TypeKey, context: Injector.BindingContext) {
        if (key.classifier != ClassScanner::class) {
            initialize(context)
            val p = providers[key]
            if (p != null) {
                context.bindToProvider(p.function.getQualifiers(), p)
            }
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
        val scanner = context.getInstance(ClassScanner::class)
        scanner.findByAnnotation(Factory::class).forEach { factoryCls ->
            if (factoryCls.isAbstract) {
                throw InjectException("@Factory class doesn't support abstract type: $factoryCls")
            }
            if (factoryCls.typeParameters.isNotEmpty()) {
                throw InjectException("@Factory class doesn't support generic type: $factoryCls")
            }
            factoryCls.declaredFunctions.forEach { f ->
                if (f.findAnnotation<Factory>() != null) {
                    if (f.typeParameters.isNotEmpty()) {
                        throw InjectException("@Factory method doesn't support type parameter: $f")
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
                        throw InjectException("No binding for parameter: $p")
                    }
                    NullProvider
                }
                1 -> BindingProvider(bindings[0])
                else -> throw InjectException("Multiple bindings for parameter: $p")
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
                    ?: throw InjectException("Factory method returned a null value: $function")
        }

        override fun toString(): String {
            return "Method($function)"
        }
    }

    class Loader : InjectorAutoLoader {
        override fun getBinders(): List<Injector.Binder> {
            return listOf(AutowireFactoryBinder())
        }
    }
}