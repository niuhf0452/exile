package com.github.niuhf0452.exile.common.internal

import com.github.niuhf0452.exile.common.ProxyFactory
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class JdkProxyFactoryBuilder<A> : AbstractProxyFactoryBuilder<A>() {
    override fun createFactory(classLoader: ClassLoader,
                               interfaces: List<Class<*>>,
                               handler: ProxyHandler<A>): ProxyFactory<A> {
        return Factory(classLoader, interfaces.toTypedArray(), handler)
    }

    private class Factory<A>(
            private val classLoader: ClassLoader,
            private val interfaces: Array<Class<*>>,
            private val handler: ProxyHandler<A>
    ) : ProxyFactory<A> {
        override fun createObject(state: A): Any {
            return Proxy.newProxyInstance(classLoader, interfaces, Handler(handler, state))
        }
    }

    private class Handler<A>(
            private val handler: ProxyHandler<A>,
            private val state: A
    ) : InvocationHandler {
        override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
            return handler.call(state, proxy, method, args)
        }
    }
}