package com.github.exile.core.impl

import com.github.exile.core.ClassScanner
import kotlin.reflect.KClass

class ClassgraphScanner(packageNames: List<String>) : ClassScanner {
    override fun findByInterface(cls: KClass<*>): Iterable<KClass<*>> {
        TODO("not implemented")
    }
}