package com.github.niuhf0452.exile.inject.impl

import com.github.niuhf0452.exile.inject.Injector
import com.github.niuhf0452.exile.inject.Provider
import com.github.niuhf0452.exile.inject.TypeKey

class NativeTypesBinder : Injector.Binder {
    override fun bind(key: TypeKey, context: Injector.BindingContext) {
        when (key.classifier) {
            Provider::class -> bindProvider(key, context)
        }
    }

    private fun bindProvider(key: TypeKey, context: Injector.BindingContext) {
        context.getBindings(key.arguments[0]).forEach { binding ->
            context.bindToInstance(binding.qualifiers, BindingProvider(binding))
        }
    }

    private class BindingProvider(
            private val binding: Injector.Binding
    ) : Provider<Any> {
        override fun get(): Any {
            return binding.getInstance()
        }
    }
}