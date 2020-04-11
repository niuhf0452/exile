package com.github.niuhf0452.exile.common

/**
 * Annotate on public API.
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.ANNOTATION_CLASS)
@Retention(value = AnnotationRetention.BINARY)
annotation class PublicApi

/**
 * Annotate on the API used for integrating between modules.
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.ANNOTATION_CLASS)
@Retention(value = AnnotationRetention.BINARY)
annotation class IntegrationApi

/**
 * This annotation is usually used with Builder pattern.
 * It annotates the methods of builder to indicate the method will return the builder itself,
 * that means the returned value and the owner of method are always the same instance.
 */
@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(value = AnnotationRetention.BINARY)
annotation class Fluent
