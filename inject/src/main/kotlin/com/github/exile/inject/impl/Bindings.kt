package com.github.exile.inject.impl

import com.github.exile.inject.Injector

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