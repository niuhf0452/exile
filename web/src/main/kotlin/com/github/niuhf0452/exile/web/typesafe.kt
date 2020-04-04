package com.github.niuhf0452.exile.web

import com.github.niuhf0452.exile.web.internal.DefaultTypeSafeHandlerInjector
import com.github.niuhf0452.exile.web.internal.TypeSafeClientHandler
import com.github.niuhf0452.exile.web.internal.TypeSafeServerHandler
import kotlin.reflect.KClass

/**
 * Define a group of HTTP endpoint.
 *
 * @param value The base path of URL.
 *
 * @since 1.0
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebEndpoint(val value: String)

/**
 * Define a HTTP endpoint.
 * This annotation can be used both in server side or client side.
 *
 * For server side, it indicates a class should be used as a type-safe request handler.
 *
 * For client side, it indicates an interface is type-safe HTTP client.
 *
 * The annotated function must be public.
 *
 * @param method The HTTP verb.
 * @param path The URL path pattern. Placeholders can be used in the pattern to capture variables.
 *
 * @since 1.0
 */
@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebMethod(val method: String, val path: String)

/**
 * A parameter in URL path.
 *
 * @param value The parameter name. If it's empty, the name of function parameter will be used as
 *              the parameter name.
 *
 * @since 1.0
 */
@MustBeDocumented
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebPathParam(val value: String = "")

/**
 * A parameter in URL query string.
 *
 * @param value The parameter name. If it's empty, the name of function parameter will be used as
 *              the parameter name.
 *
 * @since 1.0
 */
@MustBeDocumented
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebQueryParam(val value: String = "")

/**
 * A parameter in HTTP header.
 *
 * @param value The header name. If it's empty, the name of function parameter will be used as
 *              the header name.
 *
 * @since 1.0
 */
@MustBeDocumented
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebHeader(val value: String = "")

/**
 * A parameter in HTTP entity.
 *
 * @since 1.0
 */
@MustBeDocumented
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebEntity

/**
 * Add a type-safe handler to the router. The handler functions should be annotated with `@WebMethod`.
 *
 * @param cls The class owns the handler functions.
 * @param injector A injector to create the class `cls` and any injected parameters.
 *                 The default injector just simply call the constructor with reflection, so the class must has
 *                 a non-arg constructor.
 *                 This parameter is used to integrate with dependency injection library, e.g. exile-inject.
 *
 * @since 1.0
 */
fun Router.addTypeSafeHandler(cls: KClass<*>, injector: TypeSafeHandlerInjector = DefaultTypeSafeHandlerInjector) {
    TypeSafeServerHandler.addHandlers(this, cls, injector)
}

/**
 * An injector API. This is used to adapt into dependency injection library.
 *
 * @since 1.0
 */
interface TypeSafeHandlerInjector {
    fun <A : Any> getInstance(cls: KClass<A>): A
}

/**
 * A converter to convert variable of request to the parameter of function.
 *
 * For example, given following function:
 *
 * ```kotlin
 * @WebMethod("GET", "/")
 * fun get(@WebQueryParam number: Int): String
 * ```
 *
 * For server side, the value `number` in query string  of request will be extracted, and converted to Int.
 * The conversion is handled by [VariableTypeConverter].
 *
 * For client side, the function parameter is converted to string then put in the query string of request.
 * The conversion is also handled by [VariableTypeConverter].
 *
 * @since 1.0
 */
interface VariableTypeConverter<A> {
    fun parse(value: String): A
    fun stringify(value: A): String
}

/**
 * A factory of type-safe client.
 *
 * @since 1.0
 */
interface TypeSafeClientFactory<A> {
    fun getClient(client: WebClient, uri: String): A

    companion object {
        fun <A : Any> of(cls: KClass<A>): TypeSafeClientFactory<A> {
            return TypeSafeClientHandler.getClientFactory(cls)
        }
    }
}

/**
 * An exception thrown by type-safe client internally, to indicate that the response is failed, or the
 * entity can't be converted to the return type of function.
 *
 * @since 1.0
 */
class ClientResponseException(val response: WebResponse<*>, message: String, cause: Exception? = null)
    : RuntimeException(message, cause)