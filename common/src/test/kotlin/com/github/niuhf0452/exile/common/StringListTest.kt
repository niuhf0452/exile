package com.github.niuhf0452.exile.common

import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json

@OptIn(UnstableDefault::class)
class StringListTest : FunSpec({
    test("A StringList should parse string") {
        StringList.parse(" 1,2,3") shouldBe StringList(listOf("1", "2", "3"))
    }

    test("A StringList should to string") {
        StringList(listOf("1", "2", "3")).toString() shouldBe "1, 2, 3"
    }

    test("A StringList should serialize and deserialize") {
        Json.stringify(StringList.serializer(), StringList(listOf("1", "2", "3"))) shouldBe "\"1, 2, 3\""
        Json.parse(StringList.serializer(), "\" 1,2,3\"") shouldBe StringList(listOf("1", "2", "3"))
    }
})