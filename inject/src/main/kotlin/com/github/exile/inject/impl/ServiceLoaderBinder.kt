package com.github.exile.inject.impl

import com.github.exile.inject.Injector
import com.github.exile.inject.TypeKey
import java.util.*

class ServiceLoaderBinder : Injector.Binder {
    override fun bind(key: TypeKey, context: Injector.BindingContext) {
        val javaClass = key.classifier.java
        if (javaClass.isInterface && key.arguments.isEmpty()) {
            ServiceLoader.load(javaClass).stream().forEach { provider ->
                context.bindToProvider(key, emptyList()) {
                    provider.get()
                }
            }
        }
    }
}