package com.github.niuhf0452.exile.common

/**
 * Annotate on public API.
 */
@Target(AnnotationTarget.CLASS,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.ANNOTATION_CLASS)
@Retention(value = AnnotationRetention.BINARY)
annotation class PublicApi

/**
 * Annotate on the API used for integrating between modules.
 */
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
@Target(AnnotationTarget.FUNCTION)
@Retention(value = AnnotationRetention.BINARY)
annotation class Fluent