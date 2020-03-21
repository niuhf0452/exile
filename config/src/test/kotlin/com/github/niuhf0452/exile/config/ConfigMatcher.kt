package com.github.niuhf0452.exile.config

import io.kotlintest.shouldBe
import java.util.*

class ConfigMatcher {
    private val values = TreeMap<String, String>()

    fun append(name: String, value: String): ConfigMatcher {
        values[name] = value
        return this
    }

    fun shouldMatch(values: Iterable<ConfigValue>) {
        val map = TreeMap<String, String>()
        values.forEach { v ->
            map[v.path] = v.asString()
        }
        map shouldBe this.values
    }
}