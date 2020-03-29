package com.github.niuhf0452.exile.web.server

import com.github.niuhf0452.exile.web.*
import com.github.niuhf0452.exile.web.impl.RouterImpl
import com.github.niuhf0452.exile.web.impl.WebRequestImpl
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.*
import io.netty.util.concurrent.DefaultThreadFactory
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.URI
import kotlin.coroutines.CoroutineContext

class NettyServer(
        override val port: Int,
        private val bossLoop: NioEventLoopGroup,
        private val workerLoop: NioEventLoopGroup,
        router: Router
) : WebServer, Router by router {
    override fun close() {
        bossLoop.shutdownGracefully()
        workerLoop.shutdownGracefully()
    }

    class Factory : WebServer.Factory {
        override fun startServer(config: WebServer.Config, coroutineContext: CoroutineContext): WebServer {
            val router = RouterImpl(config)
            val boss = NioEventLoopGroup()
            val workerCount = Runtime.getRuntime().availableProcessors()
            val worker = NioEventLoopGroup(workerCount, DefaultThreadFactory("netty-worker", Thread.NORM_PRIORITY))
            val bootstrap = ServerBootstrap()
            val future = bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel::class.java)
                    .childHandler(HttpServerInitializer(config, router, coroutineContext))
                    .bind(config.port)
            val port = (future.sync().channel().localAddress() as InetSocketAddress).port
            return NettyServer(port, boss, worker, router)
        }
    }

    private class HttpServerInitializer(
            private val config: WebServer.Config,
            private val router: Router,
            private val context: CoroutineContext
    ) : ChannelInitializer<SocketChannel>() {
        override fun initChannel(socketChannel: SocketChannel) {
            socketChannel.pipeline()
                    .addLast(HttpServerCodec(config.maxRequestLineSize, config.maxHeaderSize, config.maxEntitySize))
                    .addLast(HttpObjectAggregator(config.maxEntitySize))
                    .addLast(HttpRequestHandler(router, context))
        }
    }

    private class HttpRequestHandler(
            private val router: Router,
            private val context: CoroutineContext
    ) : ChannelInboundHandlerAdapter() {
        @OptIn(ExperimentalCoroutinesApi::class)
        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            if (msg is FullHttpRequest) {
                if (HttpUtil.is100ContinueExpected(msg)) {
                    ctx.write(DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE))
                } else {
                    GlobalScope.launch(context = context, start = CoroutineStart.UNDISPATCHED) {
                        val response = router.onRequest(makeRequest(msg))
                        msg.release()
                        val nettyResponse = response.toNettyResponse()
                        val future = ctx.writeAndFlush(nettyResponse)
                        if (!HttpUtil.isKeepAlive(nettyResponse)) {
                            future.addListener(ChannelFutureListener.CLOSE)
                        }
                    }
                }
            } else {
                ctx.fireChannelRead(msg)
            }
        }

        private fun makeRequest(nettyRequest: FullHttpRequest): WebRequest<ByteArray> {
            val uri = URI.create(nettyRequest.uri())
            val method = nettyRequest.method().name()
            val headers = HeadersAdapter(nettyRequest.headers())
            val buf = nettyRequest.content()
            if (buf == null || buf.readableBytes() == 0) {
                return WebRequestImpl.NoEntity(uri, method, headers)
            }
            val bytes = ByteArray(buf.readableBytes())
            buf.readBytes(bytes)
            return WebRequestImpl(uri, method, headers, bytes)
        }

        private fun WebResponse<ByteArray>.toNettyResponse(): FullHttpResponse {
            val resp = if (hasEntity) {
                val content = Unpooled.wrappedBuffer(entity)
                DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(statusCode), content)
            } else {
                DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(statusCode))
            }
            headers.forEach { name ->
                resp.headers().set(name, headers.get(name))
            }
            return resp
        }
    }

    private class HeadersAdapter(
            private val headers: HttpHeaders
    ) : MultiValueMap {
        override val isEmpty: Boolean
            get() = headers.isEmpty

        override fun get(name: String): Iterable<String> {
            return headers.getAll(name)
                    ?: emptyList()
        }

        override fun iterator(): Iterator<String> {
            return headers.names().iterator()
        }
    }
}