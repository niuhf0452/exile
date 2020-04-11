package com.github.niuhf0452.exile.web

import com.github.niuhf0452.exile.web.internal.RouterImpl
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FunSpec
import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicBoolean

class RouterTest : FunSpec({
    fun WebResponse<ByteArray>.message(): String {
        statusCode shouldBe 200
        return entity!!.toString(Charsets.UTF_8)
    }

    test("A router should route by path") {
        val router = RouterImpl(WebServer.Config())
        router.addRoute("GET", "foo", TextHandler("foo"))
        router.addRoute("GET", "bar", TextHandler("bar"))
        router.onRequest(WebRequest
                .newBuilder("GET", "http://localhost/foo")
                .build())
                .message() shouldBe "foo"
        router.onRequest(WebRequest
                .newBuilder("GET", "http://localhost/bar")
                .build())
                .message() shouldBe "bar"
    }

    test("A router should route by method") {
        val router = RouterImpl(WebServer.Config())
        router.addRoute("GET", "foo", TextHandler("foo"))
        router.addRoute("PUT", "foo", TextHandler("bar"))
        router.onRequest(WebRequest
                .newBuilder("GET", "http://localhost/foo")
                .build())
                .message() shouldBe "foo"
        router.onRequest(WebRequest
                .newBuilder("PUT", "http://localhost/foo")
                .build())
                .message() shouldBe "bar"
    }

    test("A router should return 404 is no route matched") {
        val router = RouterImpl(WebServer.Config())
        router.onRequest(WebRequest
                .newBuilder("GET", "http://localhost/foo")
                .build())
                .statusCode shouldBe 404
    }

    test("A router should throw if route conflict") {
        val router = RouterImpl(WebServer.Config())
        router.addRoute("GET", "foo", TextHandler("foo"))
        shouldThrow<IllegalArgumentException> {
            router.addRoute("GET", "foo", TextHandler("bar"))
        }
    }

    test("A router should return 406 if requested media type not supported") {
        val router = RouterImpl(WebServer.Config())
        router.addRoute("GET", "foo", ObjectHandler("foo"))
        val response = router.onRequest(WebRequest
                .newBuilder("GET", "http://localhost/foo")
                .addHeader("Accept", "foo/bar")
                .build())
        response.statusCode shouldBe 406
    }

    test("A router should return 406 if requested media type mismatch returned media type") {
        val router = RouterImpl(WebServer.Config())
        router.addRoute("GET", "foo", TextHandler("foo"))
        val response = router.onRequest(WebRequest
                .newBuilder("GET", "http://localhost/foo")
                .addHeader("Accept", "application/json")
                .build())
        response.statusCode shouldBe 406
    }

    test("A router should accept */*") {
        val router = RouterImpl(WebServer.Config())
        router.addRoute("GET", "foo", TextHandler("foo"))
        val response = router.onRequest(WebRequest
                .newBuilder("GET", "http://localhost/foo")
                .addHeader("Accept", "*/*")
                .build())
        response.statusCode shouldBe 200
    }

    test("A router should return Connection: keep-alive if set keepAlive=true") {
        val router = RouterImpl(WebServer.Config(keepAlive = true))
        router.addRoute("GET", "foo", TextHandler("foo"))
        val response = router.onRequest(WebRequest
                .newBuilder("GET", "http://localhost/foo")
                .build())
        response.headers.get("Connection").firstOrNull() shouldBe "keep-alive"
    }

    test("A router should return Connection: close if set keepAlive=false") {
        val router = RouterImpl(WebServer.Config(keepAlive = false))
        router.addRoute("GET", "foo", TextHandler("foo"))
        val response = router.onRequest(WebRequest
                .newBuilder("GET", "http://localhost/foo")
                .build())
        response.headers.get("Connection").firstOrNull() shouldBe "close"
    }

    test("A router should return Connection: close if request Connection: close and set keepAlive=true") {
        val router = RouterImpl(WebServer.Config())
        router.addRoute("GET", "foo", TextHandler("foo"))
        val response = router.onRequest(WebRequest
                .newBuilder("GET", "http://localhost/foo")
                .addHeader("Connection", "close")
                .build())
        response.headers.get("Connection").firstOrNull() shouldBe "close"
    }

    test("A router should parse path variables") {
        val router = RouterImpl(WebServer.Config())
        router.addRoute("GET", "/greeting/:name", GreetingHandler())
        router.onRequest(WebRequest
                .newBuilder("GET", "http://localhost/greeting/abc")
                .build())
                .message() shouldBe "abc"
    }

    test("A router should return 500 if handler throws exception") {
        val router = RouterImpl(WebServer.Config())
        router.addRoute("GET", "/error", ErrorTriggerHandler(IllegalStateException()))
        val response = router.onRequest(WebRequest
                .newBuilder("GET", "http://localhost/error")
                .build())
        response.statusCode shouldBe 500
    }

    test("A router should return 500 if handler throws error") {
        val router = RouterImpl(WebServer.Config())
        router.addRoute("GET", "/error", ErrorTriggerHandler(AssertionError()))
        val response = router.onRequest(WebRequest
                .newBuilder("GET", "http://localhost/error")
                .build())
        response.statusCode shouldBe 500
    }

    test("A router should return 500 if ExceptionHandler throws exception") {
        val router = RouterImpl(WebServer.Config())
        router.addRoute("GET", "/error", ErrorTriggerHandler(IllegalStateException()))
        router.setExceptionHandler(object : WebExceptionHandler {
            override fun handle(exception: Throwable): WebResponse<ByteArray> {
                throw IllegalStateException()
            }
        })
        val response = router.onRequest(WebRequest
                .newBuilder("GET", "http://localhost/error")
                .build())
        response.statusCode shouldBe 500
    }

    test("A router should return Content-Length") {
        val router = RouterImpl(WebServer.Config())
        router.addRoute("GET", "/test", TextHandler("abc"))
        val response = router.onRequest(WebRequest
                .newBuilder("GET", "http://localhost/test")
                .build())
        response.statusCode shouldBe 200
        val value = response.headers.get(CommonHeaders.ContentLength).firstOrNull()
        value.shouldNotBeNull()
        value.toInt() shouldBe 3
    }

    test("A router should return Server") {
        val router = RouterImpl(WebServer.Config(serverHeader = "test"))
        router.addRoute("GET", "/test", TextHandler("abc"))
        val response = router.onRequest(WebRequest
                .newBuilder("GET", "http://localhost/test")
                .build())
        response.statusCode shouldBe 200
        response.headers.get("Server").firstOrNull() shouldBe "test"
    }

    test("A router should return Content-Type") {
        val router = RouterImpl(WebServer.Config())
        router.addRoute("GET", "/test", ObjectHandler("abc"))
        val response = router.onRequest(WebRequest
                .newBuilder("GET", "http://localhost/test")
                .build())
        response.statusCode shouldBe 200
        response.headers.get(CommonHeaders.ContentType).firstOrNull() shouldBe "application/json"
    }

    test("A router should call interceptor") {
        val called = AtomicBoolean(false)
        val router = RouterImpl(WebServer.Config())
        router.addInterceptor(object : WebInterceptor {
            override suspend fun onRequest(request: WebRequest<ByteArray>, handler: WebInterceptor.RequestHandler): WebResponse<ByteArray> {
                called.set(true)
                return handler.onRequest(request)
            }
        })
        val response = router.onRequest(WebRequest
                .newBuilder("GET", "http://localhost/test")
                .build())
        response.statusCode shouldBe 404
        called.get().shouldBeTrue()
    }
}) {
    class TextHandler(
            private val value: String
    ) : WebHandler {
        override suspend fun onRequest(context: RequestContext): WebResponse<Any> {
            return WebResponse.newBuilder()
                    .statusCode(200)
                    .addHeader(CommonHeaders.ContentType, "text/plain")
                    .entity(value)
                    .build()
        }
    }

    @Serializable
    data class ReturnValue(val value: String)

    class ObjectHandler(
            private val value: String
    ) : WebHandler {
        override suspend fun onRequest(context: RequestContext): WebResponse<Any> {
            return WebResponse.newBuilder()
                    .statusCode(200)
                    .entity(ReturnValue(value))
                    .build()
        }
    }

    class GreetingHandler : WebHandler {
        override suspend fun onRequest(context: RequestContext): WebResponse<Any> {
            val name = context.pathParams["name"]
                    ?: throw IllegalStateException("Path variable is missing: name")
            return WebResponse.newBuilder()
                    .statusCode(200)
                    .addHeader(CommonHeaders.ContentType, "text/plain")
                    .entity(name)
                    .build()
        }
    }

    class ErrorTriggerHandler(
            private val ex: Throwable
    ) : WebHandler {
        override suspend fun onRequest(context: RequestContext): WebResponse<Any> {
            throw ex
        }
    }
}