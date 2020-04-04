package com.github.niuhf0452.exile.web

import com.github.niuhf0452.exile.web.interceptor.LoggingInterceptor
import com.github.niuhf0452.exile.web.serialization.JsonEntitySerializer
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.EmptyModule
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender

class InterceptorTest : FunSpec({
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

    test("A LoggingInterceptor should output logs") {
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
}) {
    @Serializable
    data class Test(val i: Int)
}