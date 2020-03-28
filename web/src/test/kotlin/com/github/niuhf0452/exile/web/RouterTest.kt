package com.github.niuhf0452.exile.web

import com.github.niuhf0452.exile.web.impl.RouterImpl
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FunSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.EmptyModule

class RouterTest : FunSpec({
    fun WebResponse<ByteArray>.message(): String {
        return entity.toString(Charsets.UTF_8)
    }

    test("A router should route by path") {
        val router = RouterImpl(WebServer.Config(), EmptyModule)
        router.addRoute("GET", "foo", TextHandler("foo"))
        router.addRoute("GET", "bar", TextHandler("bar"))
        router.onRequest(WebRequest
                .newBuilder("http://localhost/foo")
                .method("GET")
                .noEntity())
                .message() shouldBe "foo"
        router.onRequest(WebRequest
                .newBuilder("http://localhost/bar")
                .method("GET")
                .noEntity())
                .message() shouldBe "bar"
    }

    test("A router should route by method") {
        val router = RouterImpl(WebServer.Config(), EmptyModule)
        router.addRoute("GET", "foo", TextHandler("foo"))
        router.addRoute("PUT", "foo", TextHandler("bar"))
        router.onRequest(WebRequest
                .newBuilder("http://localhost/foo")
                .method("GET")
                .noEntity())
                .message() shouldBe "foo"
        router.onRequest(WebRequest
                .newBuilder("http://localhost/foo")
                .method("PUT")
                .noEntity())
                .message() shouldBe "bar"
    }

    test("A router should return 404 is no route matched") {
        val router = RouterImpl(WebServer.Config(), EmptyModule)
        router.onRequest(WebRequest
                .newBuilder("http://localhost/foo")
                .method("GET")
                .noEntity())
                .statusCode shouldBe 404
    }

    test("A router should throw if route conflict") {
        val router = RouterImpl(WebServer.Config(), EmptyModule)
        router.addRoute("GET", "foo", TextHandler("foo"))
        shouldThrow<IllegalArgumentException> {
            router.addRoute("GET", "foo", TextHandler("bar"))
        }
    }

    test("A router should return 406 if requested media type not supported") {
        val router = RouterImpl(WebServer.Config(), EmptyModule)
        router.addRoute("GET", "foo", ObjectHandler("foo"))
        val response = router.onRequest(WebRequest
                .newBuilder("http://localhost/foo")
                .method("GET")
                .addHeader("Accept", "foo/bar")
                .noEntity())
        response.statusCode shouldBe 406
    }

    test("A router should return 406 if requested media type mismatch returned media type") {
        val router = RouterImpl(WebServer.Config(), EmptyModule)
        router.addRoute("GET", "foo", TextHandler("foo"))
        val response = router.onRequest(WebRequest
                .newBuilder("http://localhost/foo")
                .method("GET")
                .addHeader("Accept", "application/json")
                .noEntity())
        response.statusCode shouldBe 406
    }

    test("A router should accept */*") {
        val router = RouterImpl(WebServer.Config(), EmptyModule)
        router.addRoute("GET", "foo", TextHandler("foo"))
        val response = router.onRequest(WebRequest
                .newBuilder("http://localhost/foo")
                .method("GET")
                .addHeader("Accept", "*/*")
                .noEntity())
        response.statusCode shouldBe 200
    }

    test("A router should return Connection: keep-alive if set keepAlive=true") {
        val router = RouterImpl(WebServer.Config(keepAlive = true), EmptyModule)
        router.addRoute("GET", "foo", TextHandler("foo"))
        val response = router.onRequest(WebRequest
                .newBuilder("http://localhost/foo")
                .method("GET")
                .noEntity())
        response.headers.get("Connection").firstOrNull() shouldBe "keep-alive"
    }

    test("A router should return Connection: close if set keepAlive=false") {
        val router = RouterImpl(WebServer.Config(keepAlive = false), EmptyModule)
        router.addRoute("GET", "foo", TextHandler("foo"))
        val response = router.onRequest(WebRequest
                .newBuilder("http://localhost/foo")
                .method("GET")
                .noEntity())
        response.headers.get("Connection").firstOrNull() shouldBe "close"
    }

    test("A router should return Connection: close if request Connection: close and set keepAlive=true") {
        val router = RouterImpl(WebServer.Config(), EmptyModule)
        router.addRoute("GET", "foo", TextHandler("foo"))
        val response = router.onRequest(WebRequest
                .newBuilder("http://localhost/foo")
                .method("GET")
                .addHeader("Connection", "close")
                .noEntity())
        response.headers.get("Connection").firstOrNull() shouldBe "close"
    }

    test("A router should parse path variables") {
        val router = RouterImpl(WebServer.Config(), EmptyModule)
        router.addRoute("GET", "/greeting/:name", GreetingHandler())
        router.onRequest(WebRequest
                .newBuilder("http://localhost/greeting/abc")
                .method("GET")
                .noEntity())
                .message() shouldBe "abc"
    }

    test("A router should return 500 if handler throws exception") {
        val router = RouterImpl(WebServer.Config(), EmptyModule)
        router.addRoute("GET", "/error", ErrorTriggerHandler(IllegalStateException()))
        val response = router.onRequest(WebRequest
                .newBuilder("http://localhost/error")
                .method("GET")
                .noEntity())
        response.statusCode shouldBe 500
    }

    test("A router should return 500 if ExceptionHandler throws exception") {
        val router = RouterImpl(WebServer.Config(), EmptyModule)
        router.addRoute("GET", "/error", ErrorTriggerHandler(WebResponseException(Responses.NoContent)))
        router.setExceptionHandler(object : WebExceptionHandler {
            override fun handle(exception: Exception): WebResponse<ByteArray> {
                throw IllegalStateException()
            }
        })
        val response = router.onRequest(WebRequest
                .newBuilder("http://localhost/error")
                .method("GET")
                .noEntity())
        response.statusCode shouldBe 500
    }
}) {
    class TextHandler(
            private val value: String
    ) : WebHandler {
        override suspend fun onRequest(context: RequestContext, request: WebRequest<Variant>): WebResponse<Any> {
            return WebResponse.newBuilder()
                    .statusCode(200)
                    .addHeader("Content-Type", "text/plain")
                    .entity(value)
        }
    }

    @Serializable
    data class ReturnValue(val value: String)

    class ObjectHandler(
            private val value: String
    ) : WebHandler {
        override suspend fun onRequest(context: RequestContext, request: WebRequest<Variant>): WebResponse<Any> {
            return WebResponse.newBuilder()
                    .statusCode(200)
                    .entity(ReturnValue(value))
        }
    }

    class GreetingHandler : WebHandler {
        override suspend fun onRequest(context: RequestContext, request: WebRequest<Variant>): WebResponse<Any> {
            val name = context.getPathVar("name")
            return WebResponse.newBuilder()
                    .statusCode(200)
                    .addHeader("Content-Type", "text/plain")
                    .entity(name)
        }
    }

    class ErrorTriggerHandler(
            private val ex: Exception
    ) : WebHandler {
        override suspend fun onRequest(context: RequestContext, request: WebRequest<Variant>): WebResponse<Any> {
            throw ex
        }
    }
}