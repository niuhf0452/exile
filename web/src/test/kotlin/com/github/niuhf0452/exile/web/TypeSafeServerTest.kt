package com.github.niuhf0452.exile.web

import com.github.niuhf0452.exile.web.internal.RouterImpl
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec

class TypeSafeServerTest : FunSpec({
    fun WebResponse<ByteArray>.message() = entity!!.toString(Charsets.UTF_8)

    test("A router should support type safe handler") {
        val router = RouterImpl(WebServer.Config())
        router.addTypeSafeHandler(Q1::class)
        router.onRequest(WebRequest.newBuilder("GET", "http://localhost/test")
                .addHeader("Accept", "text/plain")
                .build())
                .message() shouldBe "hello"
    }

    test("A type safe handler should read path variable") {
        val router = RouterImpl(WebServer.Config())
        router.addTypeSafeHandler(Q2::class)
        router.onRequest(WebRequest.newBuilder("GET", "http://localhost/test/abc")
                .addHeader("Accept", "text/plain")
                .build())
                .message() shouldBe "hello, abc"
    }

    test("A type safe handler should read query string") {
        val router = RouterImpl(WebServer.Config())
        router.addTypeSafeHandler(Q3::class)
        router.onRequest(WebRequest.newBuilder("GET", "http://localhost/test")
                .addQueryParam("name", "abc")
                .addHeader("Accept", "text/plain")
                .build())
                .message() shouldBe "hello, abc"
    }

    test("A type safe handler should read query string with customized name") {
        val router = RouterImpl(WebServer.Config())
        router.addTypeSafeHandler(Q4::class)
        router.onRequest(WebRequest.newBuilder("GET", "http://localhost/test")
                .addQueryParam("n", "abc")
                .addHeader("Accept", "text/plain")
                .build())
                .message() shouldBe "hello, abc"
    }

    test("A type safe handler should convert parameter type") {
        val router = RouterImpl(WebServer.Config())
        router.addTypeSafeHandler(Q5::class)
        router.onRequest(WebRequest.newBuilder("GET", "http://localhost/test")
                .addQueryParam("i", "6")
                .addQueryParam("l", "100")
                .addQueryParam("d", 6.9.toString())
                .addQueryParam("b", "true")
                .addHeader("f", 3.2f.toString())
                .addHeader("Accept", "text/plain")
                .build())
                .message() shouldBe "hello, 6, 100, ${3.2}, ${6.9}, true"
    }

    test("A type safe handler should accept list variable") {
        val router = RouterImpl(WebServer.Config())
        router.addTypeSafeHandler(Q6::class)
        router.onRequest(WebRequest.newBuilder("GET", "http://localhost/test")
                .addQueryParam("i", "1")
                .addQueryParam("i", "2")
                .addQueryParam("i", "3")
                .addHeader("Accept", "text/plain")
                .build())
                .message() shouldBe "hello, [1, 2, 3]"
    }

    test("A type safe handler should accept default value") {
        val router = RouterImpl(WebServer.Config())
        router.addTypeSafeHandler(Q7::class)
        router.onRequest(WebRequest.newBuilder("GET", "http://localhost/test")
                .addHeader("Accept", "text/plain")
                .build())
                .message() shouldBe "hello, [1, 2, 3]"
    }
}) {
    @WebEndpoint("/test")
    class Q1 {
        @WebMethod("GET", "")
        fun test(): String {
            return "hello"
        }
    }

    @WebEndpoint("/test")
    class Q2 {
        @WebMethod("GET", "/:name")
        fun withName(@WebQueryParam name: String): String {
            return "hello, $name"
        }
    }

    @WebEndpoint("/test")
    class Q3 {
        @WebMethod("GET", "")
        fun test(@WebQueryParam name: String): String {
            return "hello, $name"
        }
    }

    @WebEndpoint("/test")
    class Q4 {
        @WebMethod("GET", "")
        fun test(@WebQueryParam("n") name: String): String {
            return "hello, $name"
        }
    }

    @WebEndpoint("/test")
    class Q5 {
        @WebMethod("GET", "")
        fun test(@WebQueryParam("i") i: Int,
                 @WebQueryParam("l") l: Long,
                 @WebHeader("f") f: Float,
                 @WebQueryParam("d") d: Double,
                 @WebQueryParam("b") b: Boolean): String {
            return "hello, $i, $l, $f, $d, $b"
        }
    }

    @WebEndpoint("/test")
    class Q6 {
        @WebMethod("GET", "")
        fun test(@WebQueryParam("i") i: List<Int>): String {
            return "hello, $i"
        }
    }

    @WebEndpoint("/test")
    class Q7 {
        @WebMethod("GET", "")
        fun test(@WebQueryParam("i") i: List<Int> = listOf(1, 2, 3)): String {
            return "hello, $i"
        }
    }
}