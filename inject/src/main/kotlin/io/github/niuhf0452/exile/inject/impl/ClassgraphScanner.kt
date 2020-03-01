package io.github.niuhf0452.exile.inject.impl

import io.github.classgraph.ClassGraph
import io.github.niuhf0452.exile.inject.Injector
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

class ClassgraphScanner(packageNames: List<String>) : Injector.Scanner, AutoCloseable {
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
}