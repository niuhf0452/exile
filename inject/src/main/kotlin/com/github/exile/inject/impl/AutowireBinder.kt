package com.github.exile.inject.impl

import com.github.exile.inject.Inject
import com.github.exile.inject.Injector
import com.github.exile.inject.TypeKey
import java.io.Closeable
import kotlin.reflect.full.allSupertypes

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
        if (key.classifier.isAbstract) {
            scanner.findByInterface(key.classifier).forEach { implClass ->
                if (!implClass.isAbstract && implClass.typeParameters.isEmpty()) {
                    val type = implClass.allSupertypes.find { it.classifier == key.classifier }
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
}