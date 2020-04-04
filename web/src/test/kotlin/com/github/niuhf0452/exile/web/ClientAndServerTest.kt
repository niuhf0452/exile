package com.github.niuhf0452.exile.web

import com.github.niuhf0452.exile.web.client.JdkHttpClient
import com.github.niuhf0452.exile.web.server.JettyServer
import com.github.niuhf0452.exile.web.server.NettyServer
import io.kotlintest.Spec
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

class NettyServerTest : ClientAndServerTest(JdkHttpClient.Builder(), NettyServer.Factory())

class JettyServerTest : ClientAndServerTest(JdkHttpClient.Builder(), JettyServer.Factory())

abstract class ClientAndServerTest(
        clientBuilder: WebClient.Builder,
        serverFactory: WebServer.Factory
) : FunSpec() {
    private val client = clientBuilder
            .connectTimeout(Duration.ofSeconds(1))
            .requestTimeout(Duration.ofSeconds(1))
            .maxKeepAliveConnectionSize(10)
            .build()
    private val server = serverFactory.startServer(WebServer.Config(), Dispatchers.Default)

    init {
        test("A client should send request") {
            val response = client.send(WebRequest
                    .newBuilder("GET", "http://localhost:${server.port}/test1")
                    .build())
            response.statusCode shouldBe 204
            response.entity.shouldBeNull()
        }

        test("A client should get response entity") {
            val response = client.send(WebRequest
                    .newBuilder("GET", "http://localhost:${server.port}/test2")
                    .build())
            response.statusCode shouldBe 200
            response.entity.shouldNotBeNull()
            response.entity?.convertTo(Test::class)
        }

        test("A client should send entity") {
            val response = client.send(WebRequest
                    .newBuilder("POST", "http://localhost:${server.port}/test3")
                    .addHeader(CommonHeaders.ContentType, "text/plain")
                    .entity("foo")
                    .build())
            response.statusCode shouldBe 200
            response.entity.shouldNotBeNull()
            response.entity?.convertTo(String::class) shouldBe "foo"
        }

        test("A server should accept header with multiple values") {
            val response = client.send(WebRequest
                    .newBuilder("GET", "http://localhost:${server.port}/test2")
                    .addHeader("Accept", "text/plain, application/json")
                    .build())
            response.statusCode shouldBe 200
            response.entity.shouldNotBeNull()
            response.entity?.convertTo(Test::class)
        }

        test("A server should return 404 if request not handled") {
            val response = client.send(WebRequest
                    .newBuilder("GET", "http://localhost:${server.port}/404")
                    .build())
            response.statusCode shouldBe 404
        }

        test("A client should call interceptor") {
            val called = AtomicBoolean(false)
            val client0 = clientBuilder
                    .addInterceptor(object : WebInterceptor {
                        override val order: Int
                            get() = 0

                        override suspend fun onRequest(request: WebRequest<ByteArray>, handler: WebInterceptor.RequestHandler): WebResponse<ByteArray> {
                            called.set(true)
                            return handler.onRequest(request)
                        }
                    })
                    .build()
            val response = client0.send(WebRequest
                    .newBuilder("GET", "http://localhost:${server.port}/404")
                    .build())
            response.statusCode shouldBe 404
            called.get().shouldBeTrue()
        }
    }

    override fun beforeSpec(spec: Spec) {
        val log = LoggerFactory.getLogger(this::class.java)
        log.info("Server is listening on port ${server.port}")
        val router = server.router

        router.addRoute("GET", "/test1", object : WebHandler {
            override suspend fun onRequest(context: RequestContext): WebResponse<Any> {
                return WebResponse.newBuilder().statusCode(204).build()
            }
        })

        router.addRoute("GET", "/test2", object : WebHandler {
            override suspend fun onRequest(context: RequestContext): WebResponse<Any> {
                return WebResponse.newBuilder().statusCode(200).entity(Test(123)).build()
            }
        })

        router.addRoute("POST", "/test3", object : WebHandler {
            override suspend fun onRequest(context: RequestContext): WebResponse<Any> {
                val contentType = context.request.headers.get(CommonHeaders.ContentType).firstOrNull()
                contentType shouldBe "text/plain"
                val entity = context.request.entity
                entity.shouldNotBeNull()
                val text = entity.convertTo(String::class)
                return WebResponse.newBuilder()
                        .statusCode(200)
                        .addHeader(CommonHeaders.ContentType, "text/plain")
                        .entity(text)
                        .build()
            }
        })
    }

    override fun afterSpec(spec: Spec) {
        server.close()
    }

    @Serializable
    data class Test(val value: Int)
}