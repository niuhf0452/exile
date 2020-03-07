package com.github.niuhf0452.exile.inject.impl

import com.github.niuhf0452.exile.inject.Qualifier
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.full.findAnnotation

fun KAnnotatedElement.getQualifiers(): List<Annotation> {
    return annotations.filter { a ->
        a.annotationClass.findAnnotation<Qualifier>() != null
    }
}