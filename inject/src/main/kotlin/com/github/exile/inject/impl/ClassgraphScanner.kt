package com.github.exile.inject.impl

import com.github.exile.inject.ClassScanner
import kotlin.reflect.KClass

class ClassgraphScanner(packageNames: List<String>) : ClassScanner {
    override fun findByInterface(cls: KClass<*>): Iterable<KClass<*>> {
        TODO("not implemented")
    }
}