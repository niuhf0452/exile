package com.github.niuhf0452.exile.web

import kotlinx.serialization.Serializable
import java.util.concurrent.CompletableFuture

interface WebServer : AutoCloseable {
    fun start()

    fun stop()

    fun addRoute(method: String, path: String, consumes: String, produces: String, handler: Handler)

    fun removeRoute(method: String, path: String, consumes: String, produces: String)

    override fun close() {
        stop()
    }

    interface Handler {
        suspend fun onRequest(request: WebRequest): WebResponse
    }

    interface Backend {
        fun startServer(config: Config, handler: BackendHandler): AutoCloseable
    }

    interface BackendHandler {
        fun onRequest(request: WebRequest): CompletableFuture<WebResponse>
    }

    @Serializable
    data class Config(
            val host: String,
            val port: Int,
            val contextPath: String = "/",
            val maxHeaderSizeInKB: Int = 4
    )

    companion object {
        fun bindTo(config: Config): WebServer {
            TODO()
        }
    }
}

interface WebRequest {
    val uri: WebURI
    val headers: WebHeaders
    val entity: WebEntityData
}

interface WebResponse {
    val statusCode: Int
    val headers: WebHeaders
    val entity: WebEntityData
}

interface WebURI {
    companion object {
        fun parse(uri: String): WebURI {
            TODO()
        }
    }
}

interface WebHeaders : Iterable<Pair<String, Iterable<String>>>

interface WebEntityData