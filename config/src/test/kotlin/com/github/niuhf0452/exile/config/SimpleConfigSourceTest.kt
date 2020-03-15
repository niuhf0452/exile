package com.github.niuhf0452.exile.config

import com.github.niuhf0452.exile.config.impl.SimpleConfigSource
import io.kotlintest.matchers.string.shouldStartWith
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FunSpec

class SimpleConfigSourceTest : FunSpec({
    test("A parser should parse config") {
        val source = SimpleConfigSource("""
            |text = abc
            |
            |int=1
            |section1{
            |  child1.value=}}}
            |}
            |
            |section2 {
            |  child2=   xxx239{}  
            |  child3 {
            |  
            |  }
            |  }
        """.trimMargin())
        ConfigMatcher()
                .append("text", "abc")
                .append("int", "1")
                .append("section1.child1.value", "}}}")
                .append("section2.child2", "xxx239{}")
                .shouldMatch(source.load())
    }

    test("A parser should throw if missing = or {") {
        shouldThrow<ConfigException> {
            SimpleConfigSource("text").load()
        }
    }

    test("A parser should throw if missing }") {
        shouldThrow<ConfigException> {
            SimpleConfigSource("text {").load()
        }
    }

    test("A parser should throw if name invalid") {
        shouldThrow<ConfigException> {
            SimpleConfigSource("text.* = 123").load()
        }
        shouldThrow<ConfigException> {
            SimpleConfigSource("tex. = 123").load()
        }
        shouldThrow<ConfigException> {
            SimpleConfigSource(".text-dx = 123").load()
        }
    }

    test("A parser should throw if } doesn't match {") {
        shouldThrow<ConfigException> {
            SimpleConfigSource("""
                |text {
                |}}
            """.trimMargin()).load()
        }
    }

    test("A SimpleConfigSource should implement toString") {
        SimpleConfigSource("").toString() shouldStartWith "SimpleConfigSource"
    }
})