package com.github.niuhf0452.exile.web

import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec

class WebResponseTest : FunSpec({
    test("A builder should build response") {
        var r: WebResponse<*> = WebResponse.newBuilder()
                .statusCode(204)
                .noEntity()
                .build()
        r.statusCode shouldBe 204
        r.headers.isEmpty.shouldBeTrue()
        r.entity.shouldBeNull()

        r = WebResponse.newBuilder()
                .statusCode(200)
                .addHeader("k", "q")
                .setHeader("j", listOf("123"))
                .entity(123)
                .build()
        r.statusCode shouldBe 200
        r.headers.get("k") shouldBe listOf("q")
        r.headers.get("j") shouldBe listOf("123")
        r.headers.size shouldBe 2
        r.entity shouldBe 123

        r = WebResponse.newBuilder()
                .statusCode(500)
                .addHeader("j", "123")
                .addHeader("a", "123")
                .removeHeader("j")
                .build()
        r.statusCode shouldBe 500
        r.headers.get("a") shouldBe listOf("123")
        r.headers.size shouldBe 1
        r.entity.shouldBeNull()
    }
})
