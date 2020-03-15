package com.github.niuhf0452.exile.config

import com.github.niuhf0452.exile.config.impl.ReferValueResolver
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.doubles.plusOrMinus
import io.kotlintest.matchers.string.shouldStartWith
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FunSpec

class ConfigTest : FunSpec({
    test("A Config builder should overwrite source") {
        val config = Config.newBuilder()
                .fromString("test = 123")
                .fromString("foo = bar")
                .fromString("test = 456", Config.Order.OVERWRITE)
                .build()
        config.getInt("test") shouldBe 456
        config.getString("foo") shouldBe "bar"
    }

    test("A Config builder should fallback source") {
        val config = Config.newBuilder()
                .fromString("test = 123")
                .fromString("test = 456", Config.Order.FALLBACK)
                .fromString("foo = bar", Config.Order.FALLBACK)
                .build()
        config.getInt("test") shouldBe 123
        config.getString("foo") shouldBe "bar"
    }

    test("A Config should support multiple types") {
        val config = Config.newBuilder()
                .fromString("""
                    string = abc
                    int = 123
                    long = 456
                    boolean = true
                    double = 9.32
                """.trimIndent())
                .build()
        config.getString("string") shouldBe "abc"
        config.getInt("int") shouldBe 123
        config.getLong("long") shouldBe 456L
        config.getBoolean("boolean").shouldBeTrue()
        config.getDouble("double") shouldBe 9.32.plusOrMinus(0.001)
    }

    test("A Config value should support multiple types") {
        val config = Config.newBuilder()
                .fromString("""
                    string = abc
                    int = 123
                    long = 456
                    boolean = true
                    double = 9.32
                """.trimIndent())
                .build()
        config.get("string").asString() shouldBe "abc"
        config.get("int").asInt() shouldBe 123
        config.get("long").asLong() shouldBe 456L
        config.get("boolean").asBoolean().shouldBeTrue()
        config.get("double").asDouble() shouldBe 9.32.plusOrMinus(0.001)
    }

    test("A Config value should implement toString") {
        val config = Config.newBuilder()
                .fromString("string = abc")
                .build()
        config.get("string").toString() shouldStartWith "ConfigValue"
    }

    test("A Config should read fragment") {
        val config = Config.newBuilder()
                .fromString("""
                    test.string = abc
                    test.int = 123
                    foo.bar = 123
                """.trimIndent())
                .build()
        ConfigMatcher()
                .append("string", "abc")
                .append("int", "123")
                .shouldMatch(config.getFragment("test"))
        ConfigMatcher()
                .append("test.string", "abc")
                .append("test.int", "123")
                .shouldMatch(config.getFragment("test", keepPrefix = true))

        shouldThrow<IllegalArgumentException> {
            config.getFragment("")
        }
    }

    test("A Config should read snapshot") {
        val config = Config.newBuilder()
                .fromString("""
                    test.string = abc
                    test.int = 123
                    foo.bar = 123
                """.trimIndent())
                .build()
        config.getSnapshot().toList() shouldBe config.toList()
    }

    test("A Config should resolve refer value") {
        val config = Config.newBuilder()
                .fromString("""
                    test.foo = ${'$'}{test.bar}
                    test.bar = abc
                    test.hello = Hello, ${'$'}{test.foo}!
                """.trimIndent())
                .addResolver(ReferValueResolver())
                .build()
        config.getString("test.foo") shouldBe "abc"
        config.getString("test.hello") shouldBe "Hello, abc!"
    }

    test("A Config should throw if no resolver matched") {
        shouldThrow<ConfigException> {
            Config.newBuilder()
                    .fromString("foo = ${'$'}{http://localhost:8080}")
                    .addResolver(ReferValueResolver())
                    .build()
        }
    }

    test("A Config should auto configure") {
        Config.newBuilder().autoConfigure().build()

        var config = Config.newBuilder().autoConfigure("/application-test-1.*").build()
        config.getString("text") shouldBe "abc"

        config = Config.newBuilder().autoConfigure("/application-test-2.*").build()
        config.getString("text") shouldBe "abc"
        config.getString("hello") shouldBe "world"
        config.getString("foo") shouldBe "bar"

        config = Config.newBuilder().autoConfigure("/application-test-3.*", listOf("test")).build()
        config.getString("text") shouldBe "abc"
        config.getString("foo") shouldBe "bar"
    }
})
