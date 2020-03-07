package com.github.niuhf0452.exile.inject.impl

import com.github.niuhf0452.exile.inject.Injector
import com.github.niuhf0452.exile.inject.TypeKey
import java.util.*

class ServiceLoaderBinder : Injector.Binder {
    override fun bind(key: TypeKey, context: Injector.BindingContext) {
        val javaClass = key.classifier.java
        if (javaClass.isInterface && key.arguments.isEmpty()) {
            ServiceLoader.load(javaClass).stream().forEach { provider ->
                context.bindToProvider(emptyList(), ServiceProvider(provider))
            }
        }
    }

    private class ServiceProvider(
            private val provider: ServiceLoader.Provider<*>
    ) : Injector.Provider {
        override fun getInstance(): Any {
            return provider.get()
        }

        override fun toString(): String {
            return "ServiceLoader(${provider.type()})"
        }
    }
}