package io.github.niuhf0452.exile.inject.impl

import io.github.niuhf0452.exile.inject.ClassEnhancer
import kotlin.reflect.KClass

class NoopEnhancer : ClassEnhancer {
    override fun enhance(cls: KClass<*>, methods: List<ClassEnhancer.MethodInfo>): KClass<*> {
        return cls
    }
}