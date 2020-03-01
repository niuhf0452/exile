package io.github.niuhf0452.exile.inject.impl

import io.github.niuhf0452.exile.inject.*
import kotlin.reflect.KClass
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.findAnnotation

/**
 * AutowireBinder is implementation of auto wire.
 * Auto wire is a powerful feature that automatically bind interface to the implementation class.
 * To bind interface to implementation class, the user don't need to writing code to create bindings,
 * instead just annotate the class with [@Inject][io.github.niuhf0452.exile] and enable AutowireBinder, then the AutowireBinder
 * will scan all types implement the interface and create bindings for the ones annotated with [@Inject][Inject].
 */
class AutowireBinder : Injector.Binder {
    override fun bind(key: TypeKey, context: Injector.BindingContext) {
        val cls = key.classifier
        if (cls.isAbstract && cls != Injector.Scanner::class) {
            val scanner = context.getInstance(Injector.Scanner::class)
            scanner.findBySuperClass(cls).forEach { implClass ->
                if (isAcceptable(implClass, cls)) {
                    val type = implClass.allSupertypes.find { it.classifier == cls }
                            ?: throw IllegalStateException()
                    if (key == TypeKey(type)) {
                        context.bindToType(implClass.getQualifiers(), TypeKey(implClass))
                    }
                }
            }
        }
    }

    private fun isAcceptable(implClass: KClass<*>, cls: KClass<*>): Boolean {
        val a = implClass.findAnnotation<Excludes>()
        return !implClass.isAbstract
                && implClass.typeParameters.isEmpty()
                && (a == null || !a.value.contains(cls))
    }
}