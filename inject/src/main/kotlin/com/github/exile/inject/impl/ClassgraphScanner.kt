package com.github.exile.inject.impl

import com.github.exile.inject.Injector
import io.github.classgraph.ClassGraph
import kotlin.reflect.KClass

class ClassgraphScanner(
        private val packageNames: List<String>
) : Injector.Scanner, AutoCloseable {
    private val scanResult = ClassGraph()
            .enableAnnotationInfo()
            .enableClassInfo()
            .whitelistPackages(*packageNames.toTypedArray())
            .scan()

    override fun findBySuperClass(cls: KClass<*>): Iterable<KClass<*>> {
        val classList = if (cls.java.isInterface) {
            scanResult.getClassesImplementing(cls.qualifiedName)
        } else {
            scanResult.getSubclasses(cls.qualifiedName)
        }
        return classList.loadClasses().map { it.kotlin }
    }

    override fun findByAnnotation(cls: KClass<out Annotation>): Iterable<KClass<*>> {
        return scanResult.getClassesWithAnnotation(cls.qualifiedName)
                .loadClasses()
                .map { it.kotlin }
    }

    override fun close() {
        scanResult.close()
    }
}