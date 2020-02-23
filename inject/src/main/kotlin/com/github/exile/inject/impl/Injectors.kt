package com.github.exile.inject.impl

import com.github.exile.inject.Inject
import com.github.exile.inject.Injector
import com.github.exile.inject.InjectorBuilder
import com.github.exile.inject.TypeKey
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object Injectors {
    class Eager(
            private val bindingCache: Map<TypeKey, Injector.BindingSet>
    ) : Injector {
        override fun getBindings(key: TypeKey): Injector.BindingSet {
            return bindingCache[key] ?: EmptyBindingSet
        }
    }

    class Lazy(
            private val binders: List<Injector.Binder>
    ) : Injector {
        private val bindingCache = ConcurrentHashMap<TypeKey, Injector.BindingSet>()

        override fun getBindings(key: TypeKey): Injector.BindingSet {
            return bindingCache[key] ?: synchronized(this) {
                BindingContext(binders, bindingCache).getBindings(key)
            }
        }
    }

    class Builder : InjectorBuilder {
        private var mode = Injector.LoadingMode.LAZY
        private val binders = mutableListOf<Injector.Binder>()
        private var scanner: Injector.Scanner? = null

        override fun scanner(scanner: Injector.Scanner): InjectorBuilder {
            this.scanner = scanner
            return this
        }

        override fun addBinder(binder: Injector.Binder): InjectorBuilder {
            binders.add(binder)
            return this
        }

        override fun loadingMode(mode: Injector.LoadingMode): InjectorBuilder {
            this.mode = mode
            return this
        }

        override fun enableAutowire(): InjectorBuilder {
            val scanner = this.scanner
                    ?: throw IllegalStateException("Set scanner first")
            return addBinder(AutowireBinder(scanner))
        }

        override fun enableStatic(config: (InjectorBuilder.Configurator) -> Unit): InjectorBuilder {
            return addBinder(StaticBinder(config))
        }

        override fun enableServiceLoader(): InjectorBuilder {
            return addBinder(ServiceLoaderBinder())
        }

        override fun build(): Injector {
            binders.add(InstantiateBinder())

            return when (mode) {
                Injector.LoadingMode.LAZY -> Lazy(binders)
                Injector.LoadingMode.EAGER -> {
                    val scanner = this.scanner
                            ?: throw IllegalStateException("Eager mode need a scanner")
                    val bindingCache = mutableMapOf<TypeKey, Injector.BindingSet>()
                    val context = BindingContext(binders, bindingCache)
                    scanner.findByAnnotation(Inject::class).forEach { cls ->
                        context.getBindings(TypeKey(cls))
                    }
                    Eager(bindingCache)
                }
                Injector.LoadingMode.ASYNC -> {
                    val scanner = this.scanner
                            ?: throw IllegalStateException("Eager mode need a scanner")
                    val injector = Lazy(binders)
                    Thread {
                        scanner.findByAnnotation(Inject::class).forEach { cls ->
                            injector.getBindings(TypeKey(cls))
                        }
                    }.start()
                    injector
                }
            }
        }
    }

    class BindingContext(
            private val binders: List<Injector.Binder>,
            private val bindingCache: MutableMap<TypeKey, Injector.BindingSet>
    ) : Injector.BindingContext {
        private val backtrace = Stack<TypeKey>()
        private var bindings = mutableListOf<Injector.Binding>()

        override fun bindToProvider(key: TypeKey, qualifiers: List<Annotation>, provider: () -> Any) {
            bindings.add(ProviderBinding(key, qualifiers, provider))
        }

        override fun bindToInstance(key: TypeKey, qualifiers: List<Annotation>, instance: Any) {
            if (!key.classifier.isInstance(instance)) {
                throw IllegalStateException()
            }
            bindings.add(InstanceBinding(key, qualifiers, instance))
        }

        override fun bindToType(key: TypeKey, qualifiers: List<Annotation>, implType: TypeKey) {
            if (implType.classifier == key.classifier || !key.isAssignableFrom(implType)) {
                throw IllegalStateException()
            }
            val iterator = getBindings(implType).iterator()
            if (!iterator.hasNext()) {
                throw IllegalStateException("No binding to implementation class, make sure " +
                        "${InstantiateBinder::class} is enabled: $implType")
            }
            val binding = iterator.next()
            if (iterator.hasNext()) {
                throw IllegalStateException("More than one binding found for implementation class: $implType")
            }
            bindings.add(DependencyBinding(key, qualifiers, binding))
        }

        override fun getBindings(key: TypeKey): Injector.BindingSet {
            // Don't use computeIfAbsent(), because the method will reenter.
            return bindingCache[key] ?: createBindings(key).also { bindingCache[key] = it }
        }

        private fun createBindings(key: TypeKey): Injector.BindingSet {
            if (backtrace.contains(key)) {
                throw IllegalStateException(backtrace.joinToString(
                        prefix = "Cycle dependent:\n  ->",
                        separator = "\n  ->"))
            }
            backtrace.push(key)
            val oldBindings = bindings
            try {
                bindings = mutableListOf()
                binders.forEach { binder ->
                    binder.bind(key, this)
                }
                return when (bindings.size) {
                    0 -> EmptyBindingSet
                    1 -> SingleBindingSet(bindings[0])
                    else -> ListBindingSet(Collections.unmodifiableList(bindings))
                }
            } finally {
                backtrace.pop()
                bindings = oldBindings
            }
        }
    }

    private class InstanceBinding(
            override val key: TypeKey,
            override val qualifiers: List<Annotation>,
            private val instance: Any
    ) : Injector.Binding {
        override fun getInstance(): Any {
            return instance
        }
    }

    private class ProviderBinding(
            override val key: TypeKey,
            override val qualifiers: List<Annotation>,
            private val provider: () -> Any
    ) : Injector.Binding {
        override fun getInstance(): Any {
            return provider()
        }
    }

    private class DependencyBinding(
            override val key: TypeKey,
            override val qualifiers: List<Annotation>,
            private val binding: Injector.Binding
    ) : Injector.Binding {
        override fun getInstance(): Any {
            return binding.getInstance()
        }
    }

    private class SingletonBinding(
            private val binding: Injector.Binding
    ) : Injector.Binding {
        override val key: TypeKey
            get() = binding.key
        override val qualifiers: List<Annotation>
            get() = binding.qualifiers

        private val ins by lazy { binding.getInstance() }

        override fun getInstance(): Any {
            return ins
        }
    }

    private object EmptyBindingSet : Injector.BindingSet {
        override fun getSingle(qualifiers: List<Annotation>): Injector.Binding {
            throw IllegalStateException("No binding, it's empty BindingSet")
        }

        override fun getList(qualifiers: List<Annotation>): List<Injector.Binding> {
            return emptyList()
        }

        override fun iterator(): Iterator<Injector.Binding> {
            return emptyList<Injector.Binding>().iterator()
        }
    }

    private class SingleBindingSet(
            private val binding: Injector.Binding
    ) : Injector.BindingSet {
        override fun getSingle(qualifiers: List<Annotation>): Injector.Binding {
            if (!binding.qualifiers.containsAll(qualifiers)) {
                throw IllegalStateException("No binding match the qualifiers")
            }
            return binding
        }

        override fun getList(qualifiers: List<Annotation>): List<Injector.Binding> {
            return if (binding.qualifiers.containsAll(qualifiers)) {
                listOf(binding)
            } else {
                emptyList()
            }
        }

        override fun iterator(): Iterator<Injector.Binding> {
            return object : Iterator<Injector.Binding> {
                private var next: Injector.Binding? = binding

                override fun hasNext(): Boolean {
                    return next != null
                }

                override fun next(): Injector.Binding {
                    val c = next ?: throw NoSuchElementException()
                    next = null
                    return c
                }
            }
        }
    }

    private class ListBindingSet(
            private val bindings: Iterable<Injector.Binding>
    ) : Injector.BindingSet {
        override fun getSingle(qualifiers: List<Annotation>): Injector.Binding {
            val iterator = iterator()
            val binding = iterator.findNext(qualifiers)
                    ?: throw IllegalStateException("No binding match the qualifiers")
            if (iterator.findNext(qualifiers) != null) {
                throw IllegalStateException("More than one bindings match the qualifiers")
            }
            return binding
        }

        private fun Iterator<Injector.Binding>.findNext(qualifiers: List<Annotation>): Injector.Binding? {
            while (hasNext()) {
                val binding = next()
                if (binding.qualifiers.containsAll(qualifiers)) {
                    return binding
                }
            }
            return null
        }

        override fun getList(qualifiers: List<Annotation>): List<Injector.Binding> {
            return filter { it.qualifiers.containsAll(qualifiers) }
        }

        override fun iterator(): Iterator<Injector.Binding> {
            return bindings.iterator()
        }
    }
}