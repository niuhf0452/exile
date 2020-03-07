package com.github.niuhf0452.exile.inject.impl

import com.github.niuhf0452.exile.inject.ClassScanner
import io.github.classgraph.ClassGraph
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

/**
 * An implementation of [ClassScanner] backed by Classgraph library.
 */
class ClassgraphScanner(packageNames: List<String>) : ClassScanner, AutoCloseable {
    private val scanResult = ClassGraph()
            .enableAnnotationInfo()
            .enableClassInfo()
            .whitelistPackages(*packageNames.toTypedArray())
            .scan()

    override fun findBySuperClass(cls: KClass<*>): Iterable<KClass<*>> {
        val classList = if (cls.java.isInterface) {
            scanResult.getClassesImplementing(cls.jvmName)
        } else {
            scanResult.getSubclasses(cls.jvmName)
        }
        return classList.loadClasses().map { it.kotlin }
    }

    override fun findByAnnotation(cls: KClass<out Annotation>): Iterable<KClass<*>> {
        return scanResult.getClassesWithAnnotation(cls.jvmName)
                .loadClasses()
                .map { it.kotlin }
    }

    override fun close() {
        scanResult.close()
    }

    class Factory : ClassScanner.Factory {
        override fun createScanner(packageNames: List<String>): ClassScanner {
            return ClassgraphScanner(packageNames)
        }
    }
}