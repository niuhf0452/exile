package com.github.exile.inject.impl

import com.github.exile.inject.Inject
import com.github.exile.inject.Scope

@Inject
class SingletonScope : Scope<Annotation>, Scope.Container<Annotation> {
    private var value: Any? = null

    override fun getContainer(): Scope.Container<Annotation> {
        return this
    }

    override fun getOrCreate(id: String, qualifier: Annotation, provider: () -> Any): Any {
        return value ?: synchronized(this) {
            value ?: provider().also { value = it }
        }
    }
}