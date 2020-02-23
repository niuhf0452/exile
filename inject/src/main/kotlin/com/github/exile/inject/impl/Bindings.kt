package com.github.exile.inject.impl

import com.github.exile.inject.Injector
import com.github.exile.inject.Qualifier
import com.github.exile.inject.TypeKey
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.full.findAnnotation

object Bindings {
    fun getSingle(bindingSet: Injector.BindingSet, qualifiers: List<Annotation>): Injector.Binding {
        val iterator = bindingSet.iterator()
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

    fun getList(bindingSet: Injector.BindingSet, qualifiers: List<Annotation>): List<Injector.Binding> {
        return bindingSet.filter { it.qualifiers.containsAll(qualifiers) }
    }

    class ListBindingSet(
            private val bindings: Iterable<Injector.Binding>
    ) : Injector.BindingSet {
        override fun iterator(): Iterator<Injector.Binding> {
            return bindings.iterator()
        }
    }

    class InstanceBinding(
            override val key: TypeKey,
            override val qualifiers: List<Annotation>,
            private val instance: Any
    ) : Injector.Binding {
        override fun getInstance(): Any {
            return instance
        }
    }

    class ProviderBinding(
            override val key: TypeKey,
            override val qualifiers: List<Annotation>,
            private val provider: () -> Any
    ) : Injector.Binding {
        override fun getInstance(): Any {
            return provider()
        }
    }

    class SingletonBinding(
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

    object EmptyBindingSet : Injector.BindingSet {
        override fun iterator(): Iterator<Injector.Binding> {
            return emptyList<Injector.Binding>().iterator()
        }
    }

    class CompositeBindingSet(
            private val bindingSets: List<Injector.BindingSet>
    ) : Injector.BindingSet {
        override fun iterator(): Iterator<Injector.Binding> {
            val setIterator = bindingSets.iterator()
            if (!setIterator.hasNext()) {
                return emptyList<Injector.Binding>().iterator()
            }
            return object : Iterator<Injector.Binding> {
                private var iterator: Iterator<Injector.Binding> = setIterator.next().iterator()

                override fun hasNext(): Boolean {
                    while (setIterator.hasNext() && !iterator.hasNext()) {
                        iterator = setIterator.next().iterator()
                    }
                    return iterator.hasNext()
                }

                override fun next(): Injector.Binding {
                    if (!hasNext()) {
                        throw NoSuchElementException()
                    }
                    return iterator.next()
                }
            }
        }

        class Builder {
            private val bindingSets = mutableListOf<Injector.BindingSet>()

            fun add(bindingSet: Injector.BindingSet): Builder {
                if (bindingSet !== EmptyBindingSet) {
                    bindingSets.add(bindingSet)
                }
                return this
            }

            fun build(): Injector.BindingSet {
                return when (bindingSets.size) {
                    0 -> EmptyBindingSet
                    1 -> bindingSets[0]
                    else -> CompositeBindingSet(bindingSets)
                }
            }
        }
    }

    fun KAnnotatedElement.getQualifiers(): List<Annotation> {
        return annotations.filter { a ->
            a.annotationClass.findAnnotation<Qualifier>() != null
        }
    }
}