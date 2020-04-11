package com.github.niuhf0452.exile.yaml

import io.kotlintest.matchers.collections.shouldContain
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
import java.io.ByteArrayInputStream

class YamlFileParserTest : FunSpec({
    test("A parser should accept YAML file ext names") {
        val parser = YamlFileParser()
        parser.extNames shouldContain "yaml"
        parser.extNames shouldContain "yml"
    }

    test("A parser should parse YAML file") {
        val parser = YamlFileParser()
        val file = """
            |i: 1
            |map:
            |   a: 1
            |   b: 2
            |list:
            |   - 1
            |   - 2
        """.trimMargin()
        ByteArrayInputStream(file.toByteArray()).use { i ->
            parser.parse(i)
        }.toMap() shouldBe mapOf(
                "i" to "1",
                "map.a" to "1",
                "map.b" to "2",
                "list.0" to "1",
                "list.1" to "2"
        )
    }
})