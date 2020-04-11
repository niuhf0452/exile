package com.github.niuhf0452.exile.inject.binder

import com.github.niuhf0452.exile.inject.Injector
import com.github.niuhf0452.exile.inject.InjectorAutoLoader
import com.github.niuhf0452.exile.inject.TypeKey
import com.github.niuhf0452.exile.inject.internal.getQualifiers
import java.util.*
import kotlin.reflect.jvm.jvmName

class ConfigFileBinder : Injector.Binder {
    private val configMap: Map<String, List<String>> = run {
        val map = mutableMapOf<String, MutableList<String>>()
        val e = javaClass.classLoader.getResources("inject.properties")
        while (e.hasMoreElements()) {
            e.nextElement().openStream().use { s ->
                val props = Properties()
                props.load(s)
                props.forEach { k, v ->
                    val classNames = v.toString().split(',')
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .toList()
                    map.computeIfAbsent(k.toString()) { mutableListOf() }
                            .addAll(classNames)
                }
            }
        }
        map
    }

    override fun bind(key: TypeKey, context: Injector.BindingContext) {
        val classNames = configMap[key.classifier.jvmName]
        if (classNames != null && classNames.isNotEmpty()) {
            classNames.forEach { name ->
                val cls = Class.forName(name).kotlin
                context.bindToType(cls.getQualifiers(), TypeKey(cls))
            }
        }
    }

    class Loader : InjectorAutoLoader {
        override fun getBinders(): List<Injector.Binder> {
            return listOf(ConfigFileBinder())
        }
    }
}