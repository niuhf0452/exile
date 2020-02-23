package com.github.exile.inject.impl

import com.github.exile.inject.Injector
import com.github.exile.inject.TypeKey
import java.util.*
import kotlin.streams.toList

class ServiceLoaderBinder : Injector.Binder {
    override fun bind(key: TypeKey, context: Injector.BindingContext): Injector.BindingSet {
        if (!key.classifier.java.isInterface || key.arguments.isNotEmpty()) {
            return context.emptyBindingSet()
        }
        val bindings = ServiceLoader.load(key.classifier.java)
                .stream()
                .toList()
                .map { provider ->
                    Bindings.ProviderBinding(key, emptyList()) {
                        provider.get()
                    }
                }
        return Bindings.ListBindingSet(bindings)
    }
}