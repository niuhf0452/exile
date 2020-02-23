package com.github.exile.inject.impl

import com.github.exile.inject.ClassScanner
import com.github.exile.inject.Injector
import com.github.exile.inject.TypeKey
import com.github.exile.inject.impl.Bindings.CompositeBindingSet
import kotlin.reflect.full.allSupertypes

class AutowireBinder(
        private val scanner: ClassScanner
) : Injector.Binder {
    override fun bind(key: TypeKey, context: Injector.BindingContext): Injector.BindingSet {
        if (!key.classifier.java.isInterface) {
            return context.emptyBindingSet()
        }
        val builder = CompositeBindingSet.Builder()
        scanner.findByInterface(key.classifier).forEach { implClass ->
            if (implClass.typeParameters.isEmpty()) {
                val type = implClass.allSupertypes.find { it.classifier == key.classifier }
                        ?: throw IllegalStateException()
                if (key == TypeKey(type)) {
                    val binding = context.getDependency(TypeKey(implClass))
                    builder.add(binding)
                }
            }
        }
        return builder.build()
    }
}