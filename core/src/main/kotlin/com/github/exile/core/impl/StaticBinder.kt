package com.github.exile.core.impl

import com.github.exile.core.Injector
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType

class StaticBinder(configure: (Context) -> Unit) : Injector.Binder {
    private val bindings = ConcurrentHashMap<Injector.Key, Injector.Binder>()

    init {
        configure(ContextImpl())
    }

    override fun getBinding(context: Injector.BindingContext, upstream: Injector.Binding): Injector.Binding {
        if (upstream is Injector.EmptyBinding) {
            bindings[context.key]?.let {
                return it.getBinding(context, upstream)
            }
        }
        return upstream
    }

    interface Context {
        fun bind(type: KType): QualifiedBinding<Any>
        fun <A : Any> bind(cls: KClass<A>): QualifiedBinding<A>
    }

    interface QualifiedBinding<in T : Any> : LinkBinding<T> {
        fun qualified(value: String): LinkBinding<T>
    }

    interface LinkBinding<in T : Any> : ScopeBinding {
        fun toClass(implementClass: KClass<out T>): ScopeBinding

        fun toInstance(value: T)

        fun toProvider(provider: () -> T): ScopeBinding
    }

    interface ScopeBinding {
        fun asSingleton()
    }

    private inner class ContextImpl : Context {
        override fun bind(type: KType): QualifiedBinding<Any> {
            return BindingFacade(type)
        }

        override fun <A : Any> bind(cls: KClass<A>): QualifiedBinding<A> {
            if (cls.typeParameters.isNotEmpty()) {
                throw IllegalStateException("Can't bind generic class: $cls")
            }
            return BindingFacade(cls.starProjectedType)
        }
    }

    private class InstanceBinder(
            private val instance: Any
    ) : Injector.Binder {
        override fun getBinding(context: Injector.BindingContext, upstream: Injector.Binding): Injector.Binding {
            return Bindings.InstanceBinding(instance)
        }
    }

    private class ProviderBinder(
            private val provider: () -> Any
    ) : Injector.Binder {
        override fun getBinding(context: Injector.BindingContext, upstream: Injector.Binding): Injector.Binding {
            return Bindings.ProviderBinding(provider)
        }
    }

    private class ClassBinder(
            private val cls: KClass<*>
    ) : Injector.Binder {
        override fun getBinding(context: Injector.BindingContext, upstream: Injector.Binding): Injector.Binding {
            return context.getDependency(cls.starProjectedType, "")
        }
    }

    private class SingletonBinder(
            private val binder: Injector.Binder
    ) : Injector.Binder {
        override fun getBinding(context: Injector.BindingContext, upstream: Injector.Binding): Injector.Binding {
            return Bindings.SingletonBinding(binder.getBinding(context, upstream))
        }
    }

    private inner class BindingFacade<in A : Any>(
            private val type: KType
    ) : QualifiedBinding<A>, LinkBinding<A>, ScopeBinding {
        private var qualifier = ""
        private var implementClass: KClass<out A>? = null

        override fun qualified(value: String): LinkBinding<A> {
            qualifier = value
            return this
        }

        override fun toClass(implementClass: KClass<out A>): ScopeBinding {
            if (implementClass.isSealed ||
                    implementClass.isInner ||
                    implementClass.isCompanion ||
                    implementClass.isAbstract) {
                throw IllegalArgumentException("Cannot bind to abstract class: $implementClass")
            }
            this.implementClass = implementClass
            val key = Injector.Key(type, qualifier)
            bindings[key] = ClassBinder(implementClass)
            return this
        }

        override fun toInstance(value: A) {
            val key = Injector.Key(type, qualifier)
            bindings[key] = InstanceBinder(value)
        }

        override fun toProvider(provider: () -> A): ScopeBinding {
            val key = Injector.Key(type, qualifier)
            bindings[key] = ProviderBinder(provider)
            return this
        }

        override fun asSingleton() {
            val key = Injector.Key(type, qualifier)
            bindings.computeIfPresent(key) { _, value ->
                SingletonBinder(value)
            }
        }
    }
}