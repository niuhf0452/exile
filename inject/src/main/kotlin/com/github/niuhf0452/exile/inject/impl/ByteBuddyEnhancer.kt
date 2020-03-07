package com.github.niuhf0452.exile.inject.impl

import com.github.niuhf0452.exile.inject.ClassEnhancer
import com.github.niuhf0452.exile.inject.MethodInterceptor
import net.bytebuddy.ByteBuddy
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.bind.annotation.AllArguments
import net.bytebuddy.implementation.bind.annotation.Morph
import net.bytebuddy.implementation.bind.annotation.RuntimeType
import net.bytebuddy.implementation.bind.annotation.This
import net.bytebuddy.matcher.ElementMatchers
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.jvmName


/**
 * An implementation of [ClassEnhancer] backed by ByteBuddy library.
 */
class ByteBuddyEnhancer : ClassEnhancer {
    private val buddy = ByteBuddy()
    private val superCallMorph = MethodDelegation
            .withDefaultConfiguration()
            .withBinders(Morph.Binder.install(SuperCall::class.java))

    override fun enhance(cls: KClass<*>, methods: List<ClassEnhancer.MethodInfo>): KClass<*> {
        var builder: DynamicType.Builder<out Any> = buddy.subclass(cls.java)
                .name("${cls.jvmName}\$\$Enhanced")
                .annotateType(cls.annotations)
        methods.forEach { (method, interceptors) ->
            @Suppress("UNCHECKED_CAST")
            val handler = Handler(interceptors as List<MethodInterceptor<Any, Any?>>)
            var mb = builder.method(ElementMatchers.`is`(method.javaMethod!!))
                    .intercept(superCallMorph.to(handler))
                    .annotateMethod(method.annotations)
            for (i in 1 until method.parameters.size) {
                mb = mb.annotateParameter(i - 1, method.parameters[i].annotations)
            }
            builder = mb
        }
        val subclass = builder.make()
                .load(javaClass.classLoader)
                .also { it.saveIn(File("/tmp")) }
                .loaded
        return subclass.kotlin
    }

    interface SuperCall {
        fun invoke(args: Array<Any?>): Any?
    }

    /**
     * This class MUST be public, otherwise can be called ByteBuddy.
     */
    class Handler(
            private val interceptors: List<MethodInterceptor<Any, Any?>>
    ) {
        @RuntimeType
        fun invoke(@This instance: Any, @Morph superCall: SuperCall, @AllArguments args: Array<Any?>): Any? {
            val argsList = args.toList()
            val stateList = interceptors.map { interceptor ->
                interceptor.beforeCall(instance, argsList)
            }
            var exception: Exception? = null
            val returnValue = try {
                superCall.invoke(args)
            } catch (ex: Exception) {
                exception = ex
            }
            for (i in (interceptors.size - 1) downTo 0) {
                val interceptor = interceptors[i]
                val state = stateList[i]
                interceptor.afterCall(instance, argsList, exception, returnValue, state)
            }
            if (exception != null) {
                throw exception
            }
            return returnValue
        }
    }
}