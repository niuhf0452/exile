package com.github.niuhf0452.exile.config

import com.github.niuhf0452.exile.config.source.FileSource
import io.kotlintest.matchers.string.shouldStartWith
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FunSpec
import java.nio.file.Files

class FileSourceTest : FunSpec({
    test("A FileSource should load from resource") {
        val source = Config.newBuilder().fromResource("/test.conf").build()
        ConfigMatcher()
                .append("text", "abc")
                .append("int", "1")
                .append("s.col", "true")
                .shouldMatch(source)
    }

    test("A FileSource should load conf file") {
        val config = Config.newBuilder().fromResource("test.conf").build()
        ConfigMatcher()
                .append("text", "abc")
                .append("int", "1")
                .append("s.col", "true")
                .shouldMatch(config)
    }

    test("A FileSource should load properties file") {
        val config = Config.newBuilder().fromResource("/test.properties").build()
        ConfigMatcher()
                .append("int", "2")
                .append("foo", "bar")
                .shouldMatch(config)
    }

    test("A Config builder should load from multiple resources") {
        val config = Config.newBuilder().fromResource("/test.*").build()
        ConfigMatcher()
                .append("text", "abc")
                .append("int", "1")
                .append("s.col", "true")
                .append("foo", "bar")
                .shouldMatch(config)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    test("A FileSource should load from external file") {
        val path = Files.createTempFile("config-test", ".properties")
        try {
            Files.write(path, listOf("text=abc"))
            val config = Config.newBuilder().fromFile(path.toString()).build()
            ConfigMatcher().append("text", "abc").shouldMatch(config)
        } finally {
            Files.delete(path)
        }
    }

    test("A ResourceSource should implement toString") {
        FileSource(javaClass.getResource("/test.conf"))
                .toString() shouldStartWith "FileSource"
    }

    test("A FileSource should throw if file type not supported") {
        shouldThrow<ConfigException> {
            Config.newBuilder().fromResource("/test").build()
        }
        shouldThrow<ConfigException> {
            Config.newBuilder().fromResource("/test.abc").build()
        }
    }
})