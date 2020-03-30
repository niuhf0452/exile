package com.github.niuhf0452.exile.web

import com.github.niuhf0452.exile.web.client.JdkHttpClient
import com.github.niuhf0452.exile.web.server.JettyServer
import com.github.niuhf0452.exile.web.server.NettyServer
import io.kotlintest.Spec
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
import kotlinx.coroutines.Dispatchers

class NettyServerTest : ServerTest() {
    override fun createServer(): WebServer {
        return NettyServer.Factory().startServer(WebServer.Config(), Dispatchers.Default)
    }
}

class JettyServerTest : ServerTest() {
    override fun createServer(): WebServer {
        return JettyServer.Factory().startServer(WebServer.Config(), Dispatchers.Default)
    }
}

abstract class ServerTest : FunSpec() {
    private lateinit var server: WebServer

    abstract fun createServer(): WebServer

    init {
        test("A server should respond to request") {
            val client = JdkHttpClient.Builder().build()
            client.send(WebRequest
                    .newBuilder("http://localhost:${server.port}/test")
                    .method("GET")
                    .build())
                    .statusCode shouldBe 204
        }
    }

    override fun beforeSpec(spec: Spec) {
        server = createServer()
        server.addRoute("GET", "/test", object : WebHandler {
            override suspend fun onRequest(context: RequestContext): WebResponse<Any> {
                return WebResponse.newBuilder().statusCode(204).build()
            }
        })
    }

    override fun afterSpec(spec: Spec) {
        server.close()
    }
}