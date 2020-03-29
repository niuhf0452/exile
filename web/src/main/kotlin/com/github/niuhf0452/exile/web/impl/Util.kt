package com.github.niuhf0452.exile.web.impl

object Util {
    fun joinPath(a: String, b: String): String {
        return when {
            a.isEmpty() -> b
            b.isEmpty() -> a
            else -> {
                val sb = StringBuilder(a)
                if (!sb.endsWith('/')) {
                    sb.append('/')
                }
                if (b.startsWith('/')) {
                    sb.append(b, 1, b.length)
                } else {
                    sb.append(b)
                }
                sb.toString()
            }
        }
    }
}