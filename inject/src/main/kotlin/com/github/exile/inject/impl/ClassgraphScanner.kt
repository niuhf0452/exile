package com.github.exile.inject.impl

import com.github.exile.inject.ClassScanner
import io.github.classgraph.ClassGraph
import kotlin.reflect.KClass

class ClassgraphScanner(
        private val packageNames: List<String>
) : ClassScanner {
    private val scanResult = ClassGraph()
            .enableAnnotationInfo()
            .enableClassInfo()
            .whitelistPackages(*packageNames.toTypedArray())
            .scan()
            .also { it.close() }

    override fun findByInterface(cls: KClass<*>): Iterable<KClass<*>> {
        return scanResult.getClassesImplementing(cls.qualifiedName)
                .loadClasses()
                .map { it.kotlin }
    }
}