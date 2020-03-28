package com.github.niuhf0452.exile.web.impl

import com.github.niuhf0452.exile.web.WebHeaders
import java.util.*
import kotlin.collections.ArrayList

class WebHeadersImpl() : WebHeaders {
    private val headers = TreeMap<String, MutableList<String>>(String.CASE_INSENSITIVE_ORDER)

    constructor(value: WebHeaders) : this() {
        if (value is WebHeadersImpl) {
            headers.putAll(value.headers)
        } else {
            value.forEach { name ->
                set(name, value.get(name))
            }
        }
    }

    override fun get(name: String): Iterable<String> {
        return headers[name]
                ?: emptyList()
    }

    override fun iterator(): Iterator<String> {
        return headers.keys.iterator()
    }

    fun set(value: Map<String, String>) {
        headers.clear()
        value.forEach { (k, v) ->
            val list = ArrayList<String>(1)
            list.add(v)
            headers[k] = list
        }
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