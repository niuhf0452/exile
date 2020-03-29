package com.github.niuhf0452.exile.reflection.internal

import com.github.niuhf0452.exile.reflection.ProxyFactory
import com.github.niuhf0452.exile.reflection.ProxyFactoryBuilder
import com.github.niuhf0452.exile.reflection.ProxyMethodHandler
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaMethod

abstract class AbstractProxyFactoryBuilder<A> : ProxyFactoryBuilder<A> {
    private val interfaces = mutableListOf<KClass<*>>()
    private var filter: (KFunction<*>) -> Boolean = { true }
    private var handler: (KFunction<*>) -> ProxyMethodHandler<A> = { Unsupported }

    protected abstract fun createFactory(classLoader: ClassLoader,
                                         interfaces: List<Class<*>>,
                                         handler: ProxyHandler<A>): ProxyFactory<A>

    override fun addInterface(cls: KClass<*>): ProxyFactoryBuilder<A> {
        if (!cls.java.isInterface) {
            throw IllegalArgumentException("The class should be interface: $cls")
        }
        if (!interfaces.contains(cls)) {
            interfaces.add(cls)
        }
        return this
    }

    override fun filter(f: (KFunction<*>) -> Boolean): ProxyFactoryBuilder<A> {
        filter = f
        return this
    }

    override fun handle(f: (KFunction<*>) -> ProxyMethodHandler<A>): ProxyFactoryBuilder<A> {
        handler = f
        return this
    }

    override fun build(): ProxyFactory<A> {
        if (interfaces.isEmpty()) {
            throw IllegalArgumentException("No interface added")
        }
        val handlers = mutableMapOf<Method, ProxyMethodHandler<A>>()
        handlers[Object::equals.javaMethod!!] = Equals
        handlers[Object::hashCode.javaMethod!!] = HashCode
        handlers[Object::toString.javaMethod!!] = ToString(interfaces)
        val collector = HandlerCollector(handlers)
        interfaces.forEach { cls ->
            collector.parseClass(cls)
        }
        val classLoader = Thread.currentThread().contextClassLoader
                ?: interfaces.first().java.classLoader
        val javaInterfaces = interfaces.map { it.java }
        val handler = ProxyHandler(handlers)
        return createFactory(classLoader, javaInterfaces, handler)
    }

    private inner class HandlerCollector(
            private val handlers: MutableMap<Method, ProxyMethodHandler<A>>
    ) {
        fun parseClass(cls: KClass<*>) {
            cls.members.forEach { m ->
                when (m) {
                    is KFunction<*> -> parseMethod(m)
                    is KMutableProperty<*> -> {
                        parseMethod(m.getter)
                        parseMethod(m.setter)
                    }
                    is KProperty<*> -> parseMethod(m.getter)
                    else -> throw IllegalStateException()
                }
            }
        }

        private fun parseMethod(m: KFunction<*>) {
            val javaMethod = m.javaMethod
                    ?: throw IllegalStateException("Can't find java method: $m")
            if (javaMethod !in handlers) {
                handlers[javaMethod] = when {
                    filter(m) -> handler(m)
                    javaMethod.isDefault -> JavaDefaultMethodHandler(javaMethod)
                    else -> {
                        val defaultImplClass = javaMethod.declaringClass.declaredClasses.find {
                            it.simpleName == "DefaultImpls"
                        } ?: throw IllegalArgumentException("Proxy method is not handled: $m")
                        val defaultMethod = defaultImplClass.getMethod(javaMethod.name,
                                javaMethod.declaringClass, *javaMethod.parameterTypes)
                        KotlinDefaultMethodHandler(defaultMethod)
                    }
                }
            }
        }
    }

    class ProxyHandler<A>(
            private val handlers: Map<Method, ProxyMethodHandler<A>>
    ) {
        fun call(state: A, instance: Any, method: Method, args: Array<out Any>?): Any? {
            val handler = handlers[method]
                    ?: throw IllegalStateException("The method is not handled: $method")
            try {
                return handler.call(state, instance, args)
            } catch (ex: InvocationTargetException) {
                throw ex.targetException
            }
        }
    }

    class JavaDefaultMethodHandler<A>(method: Method) : ProxyMethodHandler<A> {
        private val handle = run {
            val cls = method.declaringClass
            MethodHandles.lookup().findSpecial(cls, method.name,
                    MethodType.methodType(method.returnType, method.parameterTypes), cls)
        }

        override fun call(state: A, instance: Any, args: Array<out Any?>?): Any? {
            return when (args) {
                null -> handle.bindTo(instance).invokeWithArguments()
                else -> handle.bindTo(instance).invokeWithArguments(*args)
            }
        }
    }

    class KotlinDefaultMethodHandler<A>(
            private val method: Method
    ) : ProxyMethodHandler<A> {
        override fun call(state: A, instance: Any, args: Array<out Any?>?): Any? {
            return when (args) {
                null -> method.invoke(null, instance)
                else -> method.invoke(null, instance, *args)
            }
        }
    }

    object Unsupported : ProxyMethodHandler<Any?> {
        override fun call(state: Any?, instance: Any, args: Array<out Any?>?): Any? {
            throw UnsupportedOperationException()
        }
    }

    object Equals : ProxyMethodHandler<Any?> {
        override fun call(state: Any?, instance: Any, args: Array<out Any?>?): Any? {
            return instance === args!![0]
        }
    }

    object HashCode : ProxyMethodHandler<Any?> {
        override fun call(state: Any?, instance: Any, args: Array<out Any?>?): Any? {
            return System.identityHashCode(instance)
        }
    }

    class ToString(interfaces: List<KClass<*>>) : ProxyMethodHandler<Any?> {
        private val value = interfaces.joinToString(", ", "Proxy(", ")")

        override fun call(state: Any?, instance: Any, args: Array<out Any?>?): Any? {
            return value
        }
    }
}