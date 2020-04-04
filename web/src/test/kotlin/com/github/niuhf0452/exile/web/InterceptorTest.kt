package com.github.niuhf0452.exile.web

import com.github.niuhf0452.exile.web.interceptor.ClientContextInterceptor
import com.github.niuhf0452.exile.web.interceptor.CorsInterceptor
import com.github.niuhf0452.exile.web.interceptor.LoggingInterceptor
import com.github.niuhf0452.exile.web.interceptor.ServerContextInterceptor
import com.github.niuhf0452.exile.web.serialization.JsonEntitySerializer
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.EmptyModule
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender

class InterceptorTest : FunSpec({
    test("A LoggingInterceptor should output logs") {
        suspend fun runWith(interceptor: WebInterceptor) {
            val data = JsonEntitySerializer(EmptyModule).serialize(Test(123), MediaType.APPLICATION_JSON)
            val request = WebRequest.newBuilder("POST", "http://localhost")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Content-Length", data.size.toString())
                    .entity(data)
                    .build()
            interceptor.onRequest(request, object : WebInterceptor.RequestHandler {
                override suspend fun onRequest(request: WebRequest<ByteArray>): WebResponse<ByteArray> {
                    if (request.entity == null) {
                        return WebResponse.newBuilder().statusCode(204).build()
                    }
                    return WebResponse.newBuilder().statusCode(200)
                            .setHeader("Content-Type", request.headers.get("Content-Type"))
                            .setHeader("Content-Length", request.headers.get("Content-Length"))
                            .entity(request.entity!!)
                            .build()
                }
            })
        }

        val context = LoggerContext.getContext(false)
        val logger = context.getLogger(LoggingInterceptor::class.qualifiedName)
        val appender = ListAppender("list")
        logger.addAppender(appender)
        try {
            runWith(LoggingInterceptor(1024, emptySet()))
            appender.events.size shouldBe 2
            appender.clear()
            runWith(LoggingInterceptor(1024, setOf("localhost")))
            appender.events.shouldBeEmpty()
        } finally {
            logger.removeAppender(appender)
        }
    }

    test("A ClientContextInterceptor should send context id header") {
        val interceptor = ClientContextInterceptor()
        val request = WebRequest.newBuilder("GET", "http://localhost").build()
        val c = WorkerContext()
        c.setId("123")
        withContext(c) {
            interceptor.onRequest(request, object : WebInterceptor.RequestHandler {
                override suspend fun onRequest(request: WebRequest<ByteArray>): WebResponse<ByteArray> {
                    val id = request.headers.get(CommonHeaders.XContextId).firstOrNull()
                    id shouldBe "123"
                    return WebResponse.newBuilder().statusCode(200).build()
                }
            })
        }
    }

    test("A ServerContextInterceptor should create context") {
        val interceptor = ServerContextInterceptor()
        val request = WebRequest.newBuilder("GET", "http://localhost")
                .addHeader(CommonHeaders.XContextId, "123")
                .build()
        interceptor.onRequest(request, object : WebInterceptor.RequestHandler {
            override suspend fun onRequest(request: WebRequest<ByteArray>): WebResponse<ByteArray> {
                val context = kotlin.coroutines.coroutineContext[WorkerContext]
                context.shouldNotBeNull()
                context.getId() shouldBe "123"
                return WebResponse.newBuilder().statusCode(200).build()
            }
        })
    }

    test("A CorsInterceptor should check origin") {
        val handler = object : WebInterceptor.RequestHandler {
            override suspend fun onRequest(request: WebRequest<ByteArray>): WebResponse<ByteArray> {
                return WebResponse.newBuilder().statusCode(200).build()
            }
        }
        CorsInterceptor()
                .onRequest(WebRequest.newBuilder("GET", "http://localhost").build(), handler)
                .statusCode shouldBe 200

        CorsInterceptor()
                .onRequest(WebRequest.newBuilder("GET", "http://localhost")
                        .addHeader("Origin", "http://abc.com")
                        .build(), handler)
                .statusCode shouldBe 403

        CorsInterceptor(allowedOrigins = setOf("http://abc.com"))
                .onRequest(WebRequest.newBuilder("GET", "http://localhost")
                        .addHeader("Origin", "http://abc.com")
                        .build(), handler)
                .statusCode shouldBe 200

        CorsInterceptor(allowedOrigins = setOf("*"))
                .onRequest(WebRequest.newBuilder("GET", "http://localhost")
                        .addHeader("Origin", "http://abc.com")
                        .build(), handler)
                .statusCode shouldBe 200
    }
}) {
    @Serializable
    data class Test(val i: Int)
}