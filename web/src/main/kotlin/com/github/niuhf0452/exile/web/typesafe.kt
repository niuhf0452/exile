package com.github.niuhf0452.exile.web

import java.net.URI
import kotlin.reflect.KClass

@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebEndpoint(val value: String)

@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebMethod(
        val method: String,
        val path: String,
        val produces: String = "application/json"
)

@MustBeDocumented
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebParam(val value: String = "")

@MustBeDocumented
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebHeader(val value: String = "")

@MustBeDocumented
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebEntity(val value: String = "application/json")

interface WebClientMapper {
    fun <A : Any> get(cls: KClass<A>, baseURI: URI): A
}