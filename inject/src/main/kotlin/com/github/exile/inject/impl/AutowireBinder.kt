package com.github.exile.inject.impl

import com.github.exile.inject.Excludes
import com.github.exile.inject.Inject
import com.github.exile.inject.Injector
import com.github.exile.inject.TypeKey
import java.io.Closeable
import kotlin.reflect.KClass
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.findAnnotation

/**
 * AutowireBinder is implementation of auto wire.
 * Auto wire is a powerful feature that automatically bind interface to the implementation class.
 * To bind interface to implementation class, the user don't need to writing code to create bindings,
 * instead just annotate the class with [@Inject][Inject] and enable AutowireBinder, then the AutowireBinder
 * will scan all types implement the interface and create bindings for the ones annotated with [@Inject][Inject].
 *
 * Optionally, annotate an interface with [@Inject][Inject] if run in [EAGER][Injector.LoadingMode.EAGER] mode
 * or [ASYNC][Injector.LoadingMode.ASYNC] mode.
 */
class AutowireBinder(
        private val scanner: Injector.Scanner
) : Injector.Binder, AutoCloseable {
    override fun bind(key: TypeKey, context: Injector.BindingContext) {
        val cls = key.classifier
        if (cls.isAbstract) {
            scanner.findBySuperClass(cls).forEach { implClass ->
                if (isAcceptable(implClass, cls)) {
                    val type = implClass.allSupertypes.find { it.classifier == cls }
                            ?: throw IllegalStateException()
                    if (key == TypeKey(type)) {
                        context.bindToType(key, implClass.getQualifiers(), TypeKey(implClass))
                    }
                }
            }
        }
    }

    override fun close() {
        if (scanner is Closeable) {
            scanner.close()
        }
    }

    private fun isAcceptable(implClass: KClass<*>, cls: KClass<*>): Boolean {
        val a = implClass.findAnnotation<Excludes>()
        return !implClass.isAbstract
                && implClass.typeParameters.isEmpty()
                && (a == null || !a.value.contains(cls))
    }
}