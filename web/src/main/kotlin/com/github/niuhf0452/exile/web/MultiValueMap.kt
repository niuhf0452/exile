package com.github.niuhf0452.exile.web

import com.github.niuhf0452.exile.common.PublicApi
import java.util.*
import kotlin.collections.ArrayList

@PublicApi
class MultiValueMap(caseSensitivity: Boolean) : Iterable<String> {
    private val headers: MutableMap<String, MutableList<String>> =
            if (caseSensitivity) HashMap() else TreeMap(String.CASE_INSENSITIVE_ORDER)

    constructor(value: MultiValueMap, caseSensitivity: Boolean) : this(caseSensitivity) {
        headers.putAll(value.headers)
    }

    val isEmpty: Boolean
        get() = headers.isEmpty()

    val size: Int
        get() = headers.size

    override fun iterator(): Iterator<String> {
        return headers.keys.iterator()
    }

    fun get(name: String): Iterable<String> {
        return headers[name]
                ?: emptyList()
    }

    fun add(name: String, value: String) {
        val list = headers.computeIfAbsent(name) { ArrayList(1) }
        if (!list.contains(value)) {
            list.add(value)
        }
    }

    fun set(name: String, value: Iterable<String>) {
        val list = headers.computeIfAbsent(name) { ArrayList(1) }
        list.clear()
        value.forEach {
            if (!list.contains(it)) {
                list.add(it)
            }
        }
    }

    fun remove(name: String) {
        headers.remove(name)
    }

    fun clear() {
        headers.clear()
    }
}