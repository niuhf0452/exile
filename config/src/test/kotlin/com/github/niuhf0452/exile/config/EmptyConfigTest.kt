package com.github.niuhf0452.exile.config

import com.github.niuhf0452.exile.config.impl.EmptyConfig
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FunSpec

class EmptyConfigTest : FunSpec({
    test("An EmptyConfig can't read anything") {
        shouldThrow<ConfigException> {
            EmptyConfig.get("test")
        }
        EmptyConfig.find("test").shouldBeNull()
        EmptyConfig.iterator().hasNext().shouldBeFalse()
    }

    test("An EmptyConfig should return fragment") {
        val fragment = EmptyConfig.getFragment("test")
        fragment.iterator().hasNext().shouldBeFalse()
    }

    test("An EmptyConfig should return snapshot") {
        val fragment = EmptyConfig.getSnapshot()
        fragment.iterator().hasNext().shouldBeFalse()
    }

    test("An EmptyConfig should reload") {
        EmptyConfig.reload()
        EmptyConfig.iterator().hasNext().shouldBeFalse()
    }
})