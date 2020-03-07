package io.github.niuhf0452.exile.inject

import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/**
 * ClassInterceptor is called when injecting implementation class by [ClassEnhancer].
 * The enhancer will call [intercept] against every method in the target class
 * to see whether it need to be intercepted.
 * If a method need to be intercepted, the [intercept] should return an instance of
 * [MethodInterceptor], otherwise it should return null.
 * The [ClassEnhancer] should modify the class only when its methods need to be intercepted.
 *
 * @since 1.0
 */
interface ClassInterceptor {
    /**
     * Test whether the [method] should be intercepted, and return the [MethodInterceptor]
     * of the [method].
     *
     * @param method Tested method.
     * @return An instance of [MethodInterceptor] to handle the call of intercepted method.
     *         Or null if the method doesn't need to be intercepted.
     */
    fun intercept(method: KFunction<*>): MethodInterceptor<*, *>?
}

/**
 * MethodInterceptor handles the call to intercepted method. It can't change the parameters
 * and the return value. It can only do something before and after calling to the original method,
 * including handle the exception.
 * If a method throws exception, the exception will be caught and handled by the [MethodInterceptor],
 * but it can't be bury. That means the exception is always thrown out after calling the [MethodInterceptor]s.
 */
interface MethodInterceptor<C, S> {
    /**
     * Be called before calling to the original method.
     *
     * @param instance The instance of the method belongs to.
     * @param args The parameters of call to the intercepted method.
     * @return A state for the call. It's used to pass context between [beforeCall] and [afterCall].
     */
    fun beforeCall(instance: C, args: List<Any?>): S

    /**
     * Be called after calling to the original method.
     *
     * @param instance The instance of the method belongs to.
     * @param args The parameters of call to the intercepted method.
     * @param exception An exception thrown from the original method, or null if no exception thrown.
     * @param returnValue The return value from the original method.
     * @param state The state return from [beforeCall].
     */
    fun afterCall(instance: C, args: List<Any?>, exception: Exception?, returnValue: Any?, state: S)
}

/**
 * Enhancer is adapter interface to AOP components, e.g. CGLib, ByteBuddy.
 *
 * @since 1.0
 */
interface ClassEnhancer {
    /**
     * Modify the original class by intercepting methods with interceptors. Generally it will return
     * a subclass or modified class.
     * Note that the returned class can be loaded by a certain class loader which may be different from
     * the Thread context class loader nor the class loader of [cls].
     *
     * @param cls The intercepted class.
     * @param methods The list of methods need to be intercepted.
     * @return The subclass/modified class.
     */
    fun enhance(cls: KClass<*>, methods: List<MethodInfo>): KClass<*>

    data class MethodInfo(val method: KFunction<*>, val interceptors: List<MethodInterceptor<*, *>>)
}

/**
 * AopIgnore is a hint to ignore AOP interception for method.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
annotation class AopIgnore