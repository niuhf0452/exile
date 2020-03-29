package com.github.niuhf0452.exile.reflection

object ProxyHandlers {
    fun returnValue(value: Any?): ProxyMethodHandler<Any?> {
        return object : ProxyMethodHandler<Any?> {
            override fun call(state: Any?, instance: Any, args: Array<out Any?>?): Any? {
                return value
            }
        }
    }

    fun blackHole(): ProxyMethodHandler<Any?> {
        return returnValue(null)
    }
}