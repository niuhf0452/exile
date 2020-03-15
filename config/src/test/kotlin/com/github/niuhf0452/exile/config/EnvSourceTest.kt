package com.github.niuhf0452.exile.config

import com.github.niuhf0452.exile.config.impl.EnvironmentSource
import com.github.niuhf0452.exile.config.impl.SystemPropertiesSource
import io.kotlintest.matchers.string.shouldStartWith
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec

class EnvSourceTest : FunSpec({
    test("An EnvironmentSource should read env variables") {
        val config = Config.newBuilder()
                .fromEnvironment()
                .build()
        config.find("HOME").shouldNotBeNull()
    }

    test("An EnvironmentSource should implement toString") {
        EnvironmentSource().toString() shouldStartWith "EnvironmentSource"
    }

    test("A SystemPropertiesSource should read system properties") {
        System.setProperty("test", "123")

        val config = Config.newBuilder()
                .fromSystemProperties()
                .build()

        config.getInt("test") shouldBe 123
    }

    test("A SystemPropertiesSource should implement toString") {
        SystemPropertiesSource().toString() shouldStartWith "SystemPropertiesSource"
    }
})