package com.github.niuhf0452.exile.config

import io.kotlintest.shouldBe

object Util {
    fun matcher(): Matcher {
        return Matcher()
    }

    class Matcher {
        private val values = mutableMapOf<String, String>()

        fun append(name: String, value: String): Matcher {
            values[name] = value
            return this
        }

        fun shouldMatch(values: Iterable<ConfigValue>) {
            val map = mutableMapOf<String, String>()
            values.forEach { v ->
                map[v.path] = v.asString()
            }
            map shouldBe this.values
        }
    }
}