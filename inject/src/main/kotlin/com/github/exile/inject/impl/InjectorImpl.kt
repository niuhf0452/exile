package com.github.exile.inject.impl

import com.github.exile.inject.Injector
import com.github.exile.inject.TypeKey
import com.github.exile.inject.impl.Bindings.CompositeBindingSet
import com.github.exile.inject.impl.Bindings.EmptyBindingSet
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KType

class InjectorImpl(
        private val binders: List<Injector.Binder>
) : Injector {
    private val bindingCache = ConcurrentHashMap<TypeKey, Injector.BindingSet>()

    override fun getBindings(key: TypeKey): Injector.BindingSet {
        return bindingCache[key] ?: synchronized(this) {
            BindingContext().getDependency(key)
        }
    }

    private inner class BindingContext : Injector.BindingContext {
        private val backtrace = Stack<TypeKey>()

        override fun emptyBindingSet(): Injector.BindingSet {
            return EmptyBindingSet
        }

        override fun getDependency(type: KType): Injector.BindingSet {
            return getDependency(TypeKey(type))
        }

        override fun getDependency(key: TypeKey): Injector.BindingSet {
            // Don't use computeIfAbsent(), because the method will reenter.
            return bindingCache[key] ?: createDependency(key).also { bindingCache[key] = it }
        }

        private fun createDependency(key: TypeKey): Injector.BindingSet {
            if (backtrace.contains(key)) {
                throw IllegalStateException(backtrace.joinToString(
                        prefix = "Cycle dependent:\n  ->",
                        separator = "\n  ->"))
            }
            val builder = CompositeBindingSet.Builder()
            backtrace.push(key)
            try {
                binders.forEach { binder ->
                    builder.add(binder.bind(key, this))
                }
            } finally {
                backtrace.pop()
            }
            return builder.build()
        }
    }
}