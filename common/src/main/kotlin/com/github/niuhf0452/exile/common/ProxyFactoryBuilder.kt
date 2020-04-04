package com.github.niuhf0452.exile.common

import kotlin.reflect.KClass
import kotlin.reflect.KFunction

@PublicApi
interface ProxyFactoryBuilder<A> {
    fun addInterface(cls: KClass<*>): ProxyFactoryBuilder<A>

    fun filter(f: (KFunction<*>) -> Boolean): ProxyFactoryBuilder<A>

    fun handle(f: (KFunction<*>) -> ProxyMethodHandler<A>): ProxyFactoryBuilder<A>

    fun build(): ProxyFactory<A>
}

@PublicApi
interface ProxyMethodHandler<in A> {
    fun call(state: A, instance: Any, args: Array<out Any?>?): Any?
}

@PublicApi
interface ProxyFactory<A> {
    fun createObject(state: A): Any
}
