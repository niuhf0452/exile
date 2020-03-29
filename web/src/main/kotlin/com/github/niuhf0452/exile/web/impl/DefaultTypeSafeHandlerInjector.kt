package com.github.niuhf0452.exile.web.impl

import com.github.niuhf0452.exile.web.TypeSafeHandlerInjector
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

object DefaultTypeSafeHandlerInjector : TypeSafeHandlerInjector {
    override fun <A : Any> getInstance(cls: KClass<A>): A {
        val c = cls.primaryConstructor
                ?: throw IllegalArgumentException("Instance can't be injected: $cls")
        if (c.parameters.isNotEmpty()) {
            throw IllegalArgumentException("Instance can't be injected: $cls")
        }
        return c.call()
    }
}