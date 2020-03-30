package com.github.niuhf0452.exile.web

import com.github.niuhf0452.exile.web.client.JdkHttpClient
import com.github.niuhf0452.exile.web.server.NettyServer
import io.kotlintest.Spec
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable

class JdkHttpClientTest : ClientTest() {
    override fun newBuilder(): WebClient.Builder {
        return JdkHttpClient.Builder()
    }
}

abstract class ClientTest : FunSpec() {
    private val server = NettyServer.Factory().startServer(WebServer.Config(), Dispatchers.Default)

    init {
        test("A client should send request") {
            val client = newBuilder().build()
            val response = client.send(WebRequest.newBuilder("http://localhost:${server.port}/test1")
                    .method("GET")
                    .build())
            response.statusCode shouldBe 204
            response.hasEntity.shouldBeFalse()
        }

        test("A client should get response entity") {
            val client = newBuilder().build()
            val response = client.send(WebRequest.newBuilder("http://localhost:${server.port}/test2")
                    .method("GET")
                    .build())
            response.statusCode shouldBe 200
            response.hasEntity.shouldBeTrue()
            response.entity.convertTo(Test::class)
        }
    }

    protected abstract fun newBuilder(): WebClient.Builder

    override fun beforeSpec(spec: Spec) {
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