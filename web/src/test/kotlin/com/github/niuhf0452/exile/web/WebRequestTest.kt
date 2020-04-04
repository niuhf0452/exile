package com.github.niuhf0452.exile.web

import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec

class WebRequestTest : FunSpec({
    test("A builder should build request") {
        var r: WebRequest<*> = WebRequest
                .newBuilder("GET", "http://localhost:123/:name")
                .setPathParam("name", "foo")
                .addQueryParam("q", "123")
                .addQueryParam("q", "456")
                .addHeader("j", "123")
                .noEntity()
                .build()
        r.method shouldBe "GET"
        r.uri.toString() shouldBe "http://localhost:123/foo?q=123&q=456"
        r.headers.isEmpty.shouldBeFalse()
        r.headers.get("j") shouldBe listOf("123")
        r.headers.size shouldBe 1
        r.entity.shouldBeNull()

        r = WebRequest
                .newBuilder("PUT", "http://localhost:123/foo?q=123")
                .addQueryParam("q", "456")
                .setHeader("k", listOf("q"))
                .entity("abc")
                .build()
        r.method shouldBe "PUT"
        r.uri.toString() shouldBe "http://localhost:123/foo?q=123&q=456"
        r.headers.isEmpty.shouldBeFalse()
        r.headers.get("k") shouldBe listOf("q")
        r.headers.size shouldBe 1
        r.entity shouldBe "abc"

        r = WebRequest
                .newBuilder("DELETE", "http://localhost:123/foo")
                .addHeader("j", "123")
                .addHeader("a", "123")
                .removeHeader("j")
                .build()
        r.method shouldBe "DELETE"
        r.uri.toString() shouldBe "http://localhost:123/foo"
        r.headers.isEmpty.shouldBeFalse()
        r.headers.get("a") shouldBe listOf("123")
        r.headers.size shouldBe 1
        r.entity.shouldBeNull()
    }
})