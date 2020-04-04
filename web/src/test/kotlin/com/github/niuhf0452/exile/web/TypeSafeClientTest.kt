package com.github.niuhf0452.exile.web

import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FunSpec
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

class TypeSafeClientTest : FunSpec() {
    init {
        test("A client should respect to URI and method") {
            TypeSafeClientFactory.of(Test1::class)
                    .getClient(MockClient { request ->
                        request.method shouldBe "GET"
                        request.uri.toString() shouldBe "http://localhost/test/get"
                        WebResponse.newBuilder().statusCode(200)
                                .entity(MockVariant("abc"))
                                .build()
                    }, "http://localhost")
                    .get() shouldBe "abc"
        }

        test("A client should accept query parameter") {
            TypeSafeClientFactory.of(Test2::class)
                    .getClient(MockClient { request ->
                        request.method shouldBe "GET"
                        request.uri.toString() shouldBe "http://localhost/test/get?p=foo"
                        WebResponse.newBuilder().statusCode(200)
                                .entity(MockVariant("abc"))
                                .build()
                    }, "http://localhost")
                    .get("foo") shouldBe "abc"
        }

        test("A client should accept multiple query parameters") {
            TypeSafeClientFactory.of(Test3::class)
                    .getClient(MockClient { request ->
                        request.method shouldBe "GET"
                        request.uri.toString() shouldBe "http://localhost/get?p=foo&p=bar"
                        WebResponse.newBuilder().statusCode(200)
                                .entity(MockVariant("abc"))
                                .build()
                    }, "http://localhost")
                    .get(listOf("foo", "bar")) shouldBe "abc"
        }

        test("A client should convert type of query parameter") {
            TypeSafeClientFactory.of(Test4::class)
                    .getClient(MockClient { request ->
                        request.method shouldBe "GET"
                        request.uri.toString() shouldBe "http://localhost/get?p=123"
                        WebResponse.newBuilder().statusCode(200)
                                .entity(MockVariant("abc"))
                                .build()
                    }, "http://localhost")
                    .get(123) shouldBe "abc"
        }

        test("A client should accept customize parameter name") {
            TypeSafeClientFactory.of(Test5::class)
                    .getClient(MockClient { request ->
                        request.method shouldBe "GET"
                        request.uri.toString() shouldBe "http://localhost/get?q=true"
                        WebResponse.newBuilder().statusCode(200)
                                .entity(MockVariant("abc"))
                                .build()
                    }, "http://localhost")
                    .get(true) shouldBe "abc"
        }

        test("A client should accept path parameter") {
            TypeSafeClientFactory.of(Test6::class)
                    .getClient(MockClient { request ->
                        request.method shouldBe "GET"
                        request.uri.toString() shouldBe "http://localhost/get/fo%20o"
                        WebResponse.newBuilder().statusCode(200)
                                .entity(MockVariant("abc"))
                                .build()
                    }, "http://localhost")
                    .get("fo o") shouldBe "abc"
        }

        test("A client should accept header parameter") {
            TypeSafeClientFactory.of(Test7::class)
                    .getClient(MockClient { request ->
                        request.method shouldBe "GET"
                        request.uri.toString() shouldBe "http://localhost/get"
                        request.headers.get("test") shouldBe listOf("123")
                        WebResponse.newBuilder().statusCode(200)
                                .entity(MockVariant("abc"))
                                .build()
                    }, "http://localhost")
                    .get(123L) shouldBe "abc"
        }

        test("A client should accept entity parameter") {
            TypeSafeClientFactory.of(Test8::class)
                    .getClient(MockClient { request ->
                        request.method shouldBe "GET"
                        request.uri.toString() shouldBe "http://localhost/get"
                        request.entity shouldBe Data(123)
                        WebResponse.newBuilder().statusCode(200)
                                .entity(MockVariant("abc"))
                                .build()
                    }, "http://localhost")
                    .get(Data(123)) shouldBe "abc"
        }

        test("A client should check response status") {
            var obj = TypeSafeClientFactory.of(Test1::class)
                    .getClient(MockClient {
                        WebResponse.newBuilder().statusCode(500).build()
                    }, "http://localhost")
            shouldThrow<ClientResponseException> {
                obj.get()
            }

            obj = TypeSafeClientFactory.of(Test1::class)
                    .getClient(MockClient {
                        WebResponse.newBuilder().statusCode(204).build()
                    }, "http://localhost")
            shouldThrow<ClientResponseException> {
                obj.get()
            }

            TypeSafeClientFactory.of(Test9::class)
                    .getClient(MockClient {
                        WebResponse.newBuilder().statusCode(204).build()
                    }, "http://localhost/get")
                    .get().shouldBeNull()
        }
    }

    @WebEndpoint("/test")
    interface Test1 {
        @WebMethod("GET", "/get")
        fun get(): String
    }

    @WebEndpoint("/test")
    interface Test2 {
        @WebMethod("GET", "/get")
        fun get(@WebQueryParam p: String): String
    }

    interface Test3 {
        @WebMethod("GET", "/get")
        fun get(@WebQueryParam p: List<String>): String
    }

    interface Test4 {
        @WebMethod("GET", "/get")
        fun get(@WebQueryParam p: Int): String
    }

    interface Test5 {
        @WebMethod("GET", "/get")
        fun get(@WebQueryParam("q") p: Boolean): String
    }

    interface Test6 {
        @WebMethod("GET", "/get/:name")
        fun get(@WebPathParam name: String): String
    }

    interface Test7 {
        @WebMethod("GET", "/get")
        fun get(@WebHeader test: Long): String
    }

    interface Test8 {
        @WebMethod("GET", "/get")
        fun get(@WebEntity data: Data): String
    }

    interface Test9 {
        @WebMethod("GET", "/get")
        fun get(): String?
    }

    @Serializable
    data class Data(val i: Int)

    class MockClient(
            private val fn: (WebRequest<Any>) -> WebResponse<Variant>
    ) : WebClient {
        override suspend fun send(request: WebRequest<Any>): WebResponse<Variant> {
            return fn(request)
        }
    }

    class MockVariant(
            private val value: Any
    ) : Variant {
        override fun <T : Any> convertTo(cls: KClass<T>): T {
            value::class shouldBe cls
            @Suppress("UNCHECKED_CAST")
            return value as T
        }
    }
}