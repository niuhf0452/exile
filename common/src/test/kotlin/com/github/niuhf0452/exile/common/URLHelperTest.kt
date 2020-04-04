package com.github.niuhf0452.exile.common

import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec

class URLHelperTest : FunSpec({
    test("Helper should encode a path correctly") {
        URLHelper.encodePath("abc") shouldBe "abc"
        URLHelper.encodePath("ab c") shouldBe "ab%20c"
        URLHelper.encodePath("ab+c") shouldBe "ab%2Bc"
    }

    test("Helper should decode a path correctly") {
        URLHelper.decodePath("abc") shouldBe "abc"
        URLHelper.decodePath("ab%20c") shouldBe "ab c"
        URLHelper.decodePath("ab%2Bc") shouldBe "ab+c"
        URLHelper.decodePath("ab+c") shouldBe "ab+c"
        URLHelper.decodePath("ab c") shouldBe "ab c"
    }

    test("Helper should return the origin path if encode then decode a path") {
        URLHelper.decodePath(URLHelper.encodePath("abc")) shouldBe "abc"
        URLHelper.decodePath(URLHelper.encodePath("ab%20c")) shouldBe "ab%20c"
        URLHelper.decodePath(URLHelper.encodePath("ab%2Bc")) shouldBe "ab%2Bc"
        URLHelper.decodePath(URLHelper.encodePath("ab+c")) shouldBe "ab+c"
        URLHelper.decodePath(URLHelper.encodePath("ab c")) shouldBe "ab c"
    }

    test("Helper should encode a query string correctly") {
        URLHelper.encodeQueryString("abc") shouldBe "abc"
        URLHelper.encodeQueryString("ab c") shouldBe "ab%20c"
        URLHelper.encodeQueryString("ab+c") shouldBe "ab%2Bc"
    }

    test("Helper should decode a query string correctly") {
        URLHelper.decodeQueryString("abc") shouldBe "abc"
        URLHelper.decodeQueryString("ab%20c") shouldBe "ab c"
        URLHelper.decodeQueryString("ab%2Bc") shouldBe "ab+c"
        URLHelper.decodeQueryString("ab+c") shouldBe "ab c"
        URLHelper.decodeQueryString("ab c") shouldBe "ab c"
    }

    test("Helper should return the origin query string if encode then decode a query string") {
        URLHelper.decodeQueryString(URLHelper.encodeQueryString("abc")) shouldBe "abc"
        URLHelper.decodeQueryString(URLHelper.encodeQueryString("ab%20c")) shouldBe "ab%20c"
        URLHelper.decodeQueryString(URLHelper.encodeQueryString("ab%2Bc")) shouldBe "ab%2Bc"
        URLHelper.decodeQueryString(URLHelper.encodeQueryString("ab+c")) shouldBe "ab+c"
        URLHelper.decodeQueryString(URLHelper.encodeQueryString("ab c")) shouldBe "ab c"
    }

    test("Helper should parse query string") {
        val map = URLHelper.parseQueryString("a=1+2&b=3 4&c=%2b")
        map shouldBe mapOf("a" to "1 2", "b" to "3 4", "c" to "+")
    }
})