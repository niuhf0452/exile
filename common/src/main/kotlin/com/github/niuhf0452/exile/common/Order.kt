package com.github.niuhf0452.exile.common

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * Set the order.
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(value = AnnotationRetention.RUNTIME)
annotation class Order(val value: Int)

interface Ordered {
    fun getOrder(): Int
}

object Orders {
    const val DEFAULT = 0

    fun getOrder(value: Any): Int {
        if (value is Ordered) {
            return value.getOrder()
        }
        return getOrder(value::class)
    }

    fun getOrder(cls: KClass<*>): Int {
        val a = cls.findAnnotation<Order>()
                ?: return DEFAULT
        return a.value
    }

    fun comparator(): Comparator<Any> {
        return Comparator { o1, o2 -> getOrder(o1) - getOrder(o2) }
    }
}