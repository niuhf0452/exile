package com.github.niuhf0452.exile.web

import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec

class MediaTypeTest : FunSpec({
    test("MediaType should parse string") {
        MediaType.parse("application/json") shouldBe MediaType(
                "application/json", "application", "json")
        MediaType.parse("text/html;charset = utf-8") shouldBe MediaType(
                "text/html;charset = utf-8", "text", "html")
        MediaType.parse("*/*") shouldBe MediaType("*/*", "*", "*")
    }

    test("*/* should accept any others") {
        val any = MediaType("*/*", "*", "*")
        any.isAcceptable(MediaType.parse("application/json")).shouldBeTrue()
        any.isAcceptable(MediaType.parse("text/plain")).shouldBeTrue()
        any.isAcceptable(any).shouldBeTrue()
    }

    test("A MediaType should accept itself") {
        MediaType.parse("application/json")
                .isAcceptable(MediaType.parse("application/json"))
                .shouldBeTrue()
        MediaType.parse("text/html")
                .isAcceptable(MediaType.parse("text/html"))
                .shouldBeTrue()
    }

    test("A MediaType should parse charset") {
        MediaType.parse("application/json").charset shouldBe Charsets.UTF_8
        MediaType.parse("text/html;charset = ascii").charset shouldBe Charsets.US_ASCII
        MediaType.parse("text/html; charset=utf-16").charset shouldBe Charsets.UTF_16
        MediaType.parse("text/html;charset=Utf-16").charset shouldBe Charsets.UTF_16
    }
})