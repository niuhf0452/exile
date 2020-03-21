package com.github.niuhf0452.exile.config

import com.github.niuhf0452.exile.config.source.PropertiesSource
import io.kotlintest.matchers.string.shouldStartWith
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FunSpec
import java.util.*

class PropertiesSourceTest : FunSpec({
    test("A PropertiesSource should work") {
        val props = Properties()
        props["test"] = "abc"
        props["foo.bar"] = 123
        val config = Config.newBuilder().fromProperties(props).build()
        ConfigMatcher()
                .append("test", "abc")
                .append("foo.bar", "123")
                .shouldMatch(config)
    }

    test("A PropertiesSource should reject invalid name") {
        val props = Properties()
        props["test."] = "abc"
        shouldThrow<IllegalArgumentException> {
            PropertiesSource(props)
        }
    }

    test("A PropertiesSource should work with empty properties") {
        PropertiesSource(Properties())
    }

    test("A PropertiesSource should implement toString") {
        PropertiesSource(Properties()).toString() shouldStartWith "PropertiesSource"
    }
})