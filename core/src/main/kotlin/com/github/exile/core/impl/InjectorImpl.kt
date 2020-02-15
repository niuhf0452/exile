package com.github.exile.core.impl

import com.github.exile.core.Injector
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter

class InjectorImpl(
        private val binders: List<Injector.Binder>
) : Injector {
    private val bindingCache = ConcurrentHashMap<Injector.Key, HolderBinding>()

    override fun getBinding(type: KType, qualifier: String): Injector.Binding {
        if (type.classifier == null) {
            throw IllegalStateException("Can't inject intersection type: $type")
        }
        if (type.classifier is KTypeParameter) {
            throw IllegalStateException("Can't inject parameter type: $type")
        }
        val key = Injector.Key(type, qualifier)
        return bindingCache[key] ?: synchronized(this) {
            bindingCache[key] ?: createBinding(key)
        }
    }

    private fun createBinding(key: Injector.Key): HolderBinding {
        val dc = DependencyContext()
        val holder = dc.addDependency(key)
        bindingCache.putAll(dc.resolve())
        return holder
    }

    private inner class DependencyContext {
        private val backtrace = Stack<Injector.Key>()
        private val dependencies = mutableMapOf<Injector.Key, HolderBinding>()

        fun addDependency(key: Injector.Key): HolderBinding {
            if (backtrace.contains(key)) {
                throw IllegalStateException(backtrace.joinToString("\n  -> ",
                        "Can't create cycle dependent binding:\n  -> "))
            }
            return dependencies.computeIfAbsent(key) {
                HolderBinding()
            }
        }

        fun resolve(): Map<Injector.Key, HolderBinding> {
            val pendings = mutableListOf<Pair<Injector.Key, HolderBinding>>()
            do {
                pendings.forEach { (key, holder) ->
                    backtrace.push(key)
                    val bc = BindingContext(this, key)
                    var binding: Injector.Binding = Injector.EmptyBinding(key)
                    binding = binders.fold(binding) { upstream, binder ->
                        binder.getBinding(bc, upstream)
                    }
                    holder.binding = binding
                    backtrace.pop()
                }
                pendings.clear()
                dependencies.forEach { (key, binding) ->
                    if (binding.binding == PendingBinding) {
                        pendings.add(key to binding)
                    }
                }
            } while (pendings.isNotEmpty())
            return dependencies
        }
    }

    private inner class BindingContext(
            private val dependencyContext: DependencyContext,
            override val key: Injector.Key
    ) : Injector.BindingContext {
        override fun getDependency(type: KType, qualifier: String): Injector.Binding {
            val key = Injector.Key(type, qualifier)
            return bindingCache[key] ?: dependencyContext.addDependency(key)
        }
    }

    private class HolderBinding : Injector.Binding {
        var binding: Injector.Binding = PendingBinding

        override fun getInstance(): Any? {
            return binding.getInstance()
        }
    }

    object PendingBinding : Injector.Binding {
        override fun getInstance(): Any? {
            throw IllegalStateException("HolderBinding is not ready")
        }
    }
}

class InjectorBuilder : Injector.Builder {
    private val binders = mutableListOf<Injector.Binder>()

    override fun addBinder(binder: Injector.Binder): Injector.Builder {
        binders.add(binder)
        return this
    }

    override fun build(): Injector {
        return InjectorImpl(binders)
    }
}