package com.github.exile.core.impl

import com.github.exile.core.Injector

object Bindings {
    class InstanceBinding(
            private val instance: Any
    ) : Injector.Binding {
        override fun getInstance(): Any? {
            return instance
        }
    }


    class ProviderBinding(
            private val provider: () -> Any
    ) : Injector.Binding {
        override fun getInstance(): Any? {
            return provider()
        }
    }

    class SingletonBinding(
            private val binding: Injector.Binding
    ) : Injector.Binding {
        private val ins by lazy { binding.getInstance() }

        override fun getInstance(): Any? {
            return ins
        }
    }
}