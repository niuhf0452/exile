package com.github.niuhf0452.exile.config

import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FunSpec
import kotlinx.serialization.Serializable

class SimpleConfigTest : FunSpec({
    test("A SimpleConfig should serialize object") {
        val data = Data(
                123,
                "abc",
                listOf("a", "b"),
                listOf(Value(300, true)),
                mapOf("foo" to "bar"),
                mapOf(
                        "foo" to Value(100, false),
                        "bar" to Value(200, true)
                ))
        val config = Config.toConfig(Data.serializer(), data)
        ConfigMatcher()
                .append("i", "123")
                .append("s", "abc")
                .append("list.0", "a")
                .append("list.1", "b")
                .append("list2.0.j", "300")
                .append("list2.0.k", "true")
                .append("map.foo", "bar")
                .append("map2.foo.j", "100")
                .append("map2.foo.k", "false")
                .append("map2.bar.j", "200")
                .append("map2.bar.k", "true")
                .shouldMatch(config)
    }

    test("A SimpleConfig should deserialize object") {
        val config = Config.newBuilder()
                .fromString("""
                    i = 123
                    s = abc
                    list.0 = a
                    list.1 = b
                    list2.0.j = 300
                    list2.0.k = false
                    map.foo = bar 
                    map2.foo.j = 100
                    map2.foo.k = true
                    map2.bar.j = 200
                    map2.bar.k = false
                """.trimIndent())
                .build()
        val data = config.parse(Data.serializer())
        data shouldBe Data(
                123,
                "abc",
                listOf("a", "b"),
                listOf(Value(300, false)),
                mapOf("foo" to "bar"),
                mapOf(
                        "foo" to Value(100, true),
                        "bar" to Value(200, false)
                ))
    }

    test("A SimpleConfig should throw if serialize map with complex key") {
        shouldThrow<IllegalArgumentException> {
            Config.toConfig(Data2.serializer(), Data2(mapOf(Value(1, true) to Value(2, false))))
        }
    }

    test("A SimpleConfig should throw if deserialize map with complex key") {
        shouldThrow<IllegalArgumentException> {
            val config = Config.newBuilder()
                    .fromString("""
                      m.bar.j = 200
                       m.bar.k = false
                     """.trimIndent())
                    .build()
            config.parse(Data2.serializer())
        }
    }
}) {
    @Serializable
    data class Data(val i: Int,
                    val s: String,
                    val list: List<String>,
                    val list2: List<Value>,
                    val map: Map<String, String>,
                    val map2: Map<String, Value>)

    @Serializable
    data class Value(val j: Int, val k: Boolean)

    @Serializable
    data class Data2(val m: Map<Value, Value>)
}