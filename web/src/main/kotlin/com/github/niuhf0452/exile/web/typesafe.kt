package com.github.niuhf0452.exile.web

import com.github.niuhf0452.exile.web.impl.DefaultTypeSafeHandlerInjector
import com.github.niuhf0452.exile.web.impl.TypeSafeClientHandler
import com.github.niuhf0452.exile.web.impl.TypeSafeServerHandler
import kotlin.reflect.KClass

@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebEndpoint(val value: String)

@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebMethod(val method: String, val path: String)

@MustBeDocumented
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebPathParam(val value: String = "")

@MustBeDocumented
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebQueryParam(val value: String = "")

@MustBeDocumented
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebHeader(val value: String = "")

@MustBeDocumented
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebEntity(val value: String = "application/json")

fun Router.addTypeSafeHandler(cls: KClass<*>, injector: TypeSafeHandlerInjector = DefaultTypeSafeHandlerInjector) {
    TypeSafeServerHandler.addHandlers(this, cls, injector)
}

interface TypeSafeHandlerInjector {
    fun <A : Any> getInstance(cls: KClass<A>): A
}

interface VariableTypeConverter<A> {
    fun parse(value: String): A
    fun stringify(value: A): String
}

interface TypeSafeClientFactory<A> {
    fun getClient(client: WebClient, uri: String): A

    companion object {
        fun <A : Any> of(cls: KClass<A>): TypeSafeClientFactory<A> {
            return TypeSafeClientHandler.getClientFactory(cls)
        }
    }
}