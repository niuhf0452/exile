package com.github.niuhf0452.exile.web

import com.github.niuhf0452.exile.web.client.JdkHttpClient
import com.github.niuhf0452.exile.web.server.JettyServer
import com.github.niuhf0452.exile.web.server.NettyServer
import io.kotlintest.Spec
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

class JdkHttpClientTest : ClientAndServerTest(JdkHttpClient.Builder(), JettyServer.Factory())

class NettyServerTest : ClientAndServerTest(JdkHttpClient.Builder(), NettyServer.Factory())

class JettyServerTest : ClientAndServerTest(JdkHttpClient.Builder(), JettyServer.Factory())

abstract class ClientAndServerTest(
        clientBuilder: WebClient.Builder,
        serverFactory: WebServer.Factory
) : FunSpec() {
    private val client = clientBuilder.build()
    private val server = serverFactory.startServer(WebServer.Config(), Dispatchers.Default)

    init {
        test("A client should send request") {
            val response = client.send(WebRequest
                    .newBuilder("GET","http://localhost:${server.port}/test1")
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
    }

    override fun beforeSpec(spec: Spec) {
        val log = LoggerFactory.getLogger(this::class.java)
        log.info("Server is listening on port ${server.port}")

        server.addRoute("GET", "/test1", object : WebHandler {
            override suspend fun onRequest(context: RequestContext): WebResponse<Any> {
                return WebResponse.newBuilder().statusCode(204).build()
            }
        })

        server.addRoute("GET", "/test2", object : WebHandler {
            override suspend fun onRequest(context: RequestContext): WebResponse<Any> {
                return WebResponse.newBuilder().statusCode(200).entity(Test(123)).build()
            }
        })
    }

    override fun afterSpec(spec: Spec) {
        server.close()
    }

    @Serializable
    data class Test(val value: Int)
}