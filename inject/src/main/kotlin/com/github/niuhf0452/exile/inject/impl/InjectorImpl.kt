package com.github.niuhf0452.exile.inject.impl

import com.github.niuhf0452.exile.inject.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList

class InjectorImpl(
        private val binders: List<Injector.Binder>,
        private val filters: List<Injector.Filter>,
        private val bindingCache: MutableMap<TypeKey, Injector.BindingSet>
) : Injector {
    private var closed = false

    override fun getBindings(key: TypeKey): Injector.BindingSet {
        checkClosed()
        return bindingCache[key] ?: run {
            if (binders.isNotEmpty()) {
                synchronized(this) {
                    checkClosed()
                    BindingContext(binders, filters, bindingCache)
                            .withContext { c ->
                                c.getBindings(key)
                            }
                }
            } else {
                EmptyBindingSet
            }
        }
    }

    override fun preparedBindings(): List<Injector.Binding> {
        checkClosed()
        val bindings = mutableListOf<Injector.Binding>()
        bindingCache.values.forEach { bindings.addAll(it) }
        return bindings
    }

    override fun close() {
        synchronized(this) {
            if (!closed) {
                closed = true
                binders.forEach { (it as? AutoCloseable)?.close() }
                filters.forEach { (it as? AutoCloseable)?.close() }
                bindingCache.clear()
            }
        }
    }

    private fun checkClosed() {
        if (closed) {
            throw IllegalStateException("Injector is closed")
        }
    }

    companion object {
        val contextHolder = ThreadLocal<Injector.BindingContext>()

        fun <R> BindingContext.withContext(f: (c: BindingContext) -> R): R {
            val old = contextHolder.get()
            try {
                contextHolder.set(this)
                return f(this)
            } finally {
                contextHolder.set(old)
            }
        }
    }

    class Builder : InjectorBuilder {
        private val packageNames = mutableListOf<String>()
        private val binders = mutableListOf<Injector.Binder>()
        private val filters = mutableListOf<Injector.Filter>()
        private var scannerFactory: ClassScanner.Factory? = null
        private var enhancer: ClassEnhancer? = null
        private val interceptors = mutableListOf<ClassInterceptor>()

        override fun addPackage(packageName: String): InjectorBuilder {
            packageNames.add(packageName)
            return this
        }

        override fun scanner(scannerFactory: ClassScanner.Factory): InjectorBuilder {
            this.scannerFactory = scannerFactory
            return this
        }

        override fun enhancer(enhancer: ClassEnhancer): InjectorBuilder {
            this.enhancer = enhancer
            return this
        }

        override fun addInterceptor(interceptor: ClassInterceptor): InjectorBuilder {
            interceptors.add(interceptor)
            return this
        }

        override fun addBinder(binder: Injector.Binder): InjectorBuilder {
            binders.add(binder)
            return this
        }

        override fun addFilter(filter: Injector.Filter): InjectorBuilder {
            filters.add(filter)
            return this
        }

        override fun enableAutowire(): InjectorBuilder {
            addBinder(AutowireBinder())
            addBinder(AutowireFactoryBinder())
            return this
        }

        override fun enableStatic(config: (InjectorBuilder.Configurator) -> Unit): InjectorBuilder {
            return addBinder(StaticBinder(config))
        }

        override fun enableServiceLoader(): InjectorBuilder {
            return addBinder(ServiceLoaderBinder())
        }

        override fun enableScope(): InjectorBuilder {
            return addFilter(ScopeSupportFilter())
        }

        override fun build(): Injector {
            val packageNames = ArrayList(this.packageNames)
            if (packageNames.isEmpty()) {
                throw IllegalArgumentException("package name not set")
            }
            val binders = ArrayList(this.binders)
            val enhancer = this.enhancer ?: NoopEnhancer()
            val scannerFactory = this.scannerFactory ?: ClassgraphScanner.Factory()
            binders.add(ScannerBinder(scannerFactory.createScanner(packageNames)))
            binders.add(InstantiateBinder(packageNames, enhancer, ArrayList(interceptors)))
            binders.add(NativeTypesBinder())
            val filters = ArrayList(this.filters)
            return InjectorImpl(binders, filters, ConcurrentHashMap())
        }
    }

    class BindingContext(
            private val binders: List<Injector.Binder>,
            private val filters: List<Injector.Filter>,
            private val bindingCache: MutableMap<TypeKey, Injector.BindingSet>
    ) : Injector.BindingContext {
        private val backtrace = Stack<TypeKey>()
        private var bindings = mutableListOf<Injector.Binding>()

        override fun bindToProvider(qualifiers: List<Annotation>, provider: Injector.Provider) {
            val key = backtrace.peek()
            addBinding(ProviderBinding(key, qualifiers, provider))
        }

        override fun bindToInstance(qualifiers: List<Annotation>, instance: Any) {
            val key = backtrace.peek()
            if (!key.classifier.isInstance(instance)) {
                throw IllegalArgumentException("The instance is not compatible with the bind type: " +
                        "instance type = ${instance::class}, bind type = $key")
            }
            addBinding(InstanceBinding(key, qualifiers, instance))
        }

        override fun bindToType(qualifiers: List<Annotation>, implType: TypeKey) {
            val key = backtrace.peek()
            if (implType.classifier == key.classifier || !key.isAssignableFrom(implType)) {
                throw IllegalArgumentException("The implType is not compatible with the bind type: " +
                        "implType = $implType, bind type = $key")
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
            addBinding(DependencyBinding(key, qualifiers, binding))
        }

        private fun addBinding(binding: Injector.Binding) {
            val filtered = filters.fold(binding) { b, f ->
                val nb = f.filter(b)
                if (nb.key != b.key) {
                    throw IllegalStateException("A Filter should NOT change the binding type: filter = $f, " +
                            "input type = ${b.key}, output type = ${nb.key}")
                }
                nb
            }
            bindings.add(filtered)
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

        override fun toString(): String {
            return "Instance(key = $key, qualifiers = [${qualifiers.joinToString(", ")}], instance = $instance)"
        }
    }

    private class ProviderBinding(
            override val key: TypeKey,
            override val qualifiers: List<Annotation>,
            private val provider: Injector.Provider
    ) : Injector.Binding {
        override fun getInstance(): Any {
            return provider.getInstance()
        }

        override fun toString(): String {
            return "Provider(key = $key, qualifiers = [${qualifiers.joinToString(", ")}], provider = $provider)"
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

        override fun toString(): String {
            return "Dependency(key = $key, qualifiers = [${qualifiers.joinToString(", ")}], binding = $binding)"
        }
    }

    private object EmptyBindingSet : Injector.BindingSet {
        override fun getSingle(qualifiers: List<Annotation>): Injector.Binding {
            throw IllegalArgumentException("No binding, it's empty BindingSet")
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
                throw IllegalArgumentException("No binding match the qualifiers")
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
                    ?: throw IllegalArgumentException("No binding match the qualifiers")
            if (iterator.findNext(qualifiers) != null) {
                throw IllegalArgumentException("More than one bindings match the qualifiers")
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