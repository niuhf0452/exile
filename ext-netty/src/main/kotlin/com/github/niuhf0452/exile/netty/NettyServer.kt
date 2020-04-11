package com.github.niuhf0452.exile.netty

import com.github.niuhf0452.exile.common.PublicApi
import com.github.niuhf0452.exile.web.*
import com.github.niuhf0452.exile.web.internal.RouterImpl
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
        private val bossLoop: NioEventLoopGroup,
        private val workerLoop: NioEventLoopGroup,
        override val port: Int,
        override val router: Router
) : WebServer {
    override fun close() {
        bossLoop.shutdownGracefully()
        workerLoop.shutdownGracefully()
    }

    @PublicApi
    class Factory : WebServer.Factory {
        override fun startServer(config: WebServer.Config, coroutineContext: CoroutineContext, router: Router?): WebServer {
            val router0 = router ?: RouterImpl(config)
            val boss = NioEventLoopGroup()
            val workerCount = Runtime.getRuntime().availableProcessors()
            val worker = NioEventLoopGroup(workerCount, DefaultThreadFactory("netty-worker", Thread.NORM_PRIORITY))
            val bootstrap = ServerBootstrap()
            val future = bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel::class.java)
                    .childHandler(HttpServerInitializer(config, router0, coroutineContext))
                    .bind(config.port)
            val port = (future.sync().channel().localAddress() as InetSocketAddress).port
            return NettyServer(boss, worker, port, router0)
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
            val headers = readHeaders(nettyRequest.headers())
            val buf = nettyRequest.content()
            if (buf == null || buf.readableBytes() == 0) {
                return WebRequest(uri, method, headers, null)
            }
            val bytes = ByteArray(buf.readableBytes())
            buf.readBytes(bytes)
            return WebRequest(uri, method, headers, bytes)
        }

        private fun readHeaders(nettyHeaders: HttpHeaders): MultiValueMap {
            val headers = MultiValueMap(false)
            nettyHeaders.iteratorAsString().forEach { (k, v) ->
                v.split(',').forEach { value ->
                    val v0 = value.trim()
                    if (v0.isNotEmpty()) {
                        headers.add(k, v0)
                    }
                }
            }
            return headers
        }

        private fun WebResponse<ByteArray>.toNettyResponse(): FullHttpResponse {
            val resp = if (entity == null) {
                DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(statusCode))
            } else {
                val content = Unpooled.wrappedBuffer(entity)
                DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(statusCode), content)
            }
            headers.forEach { name ->
                resp.headers().set(name, headers.get(name))
            }
            return resp
        }
    }
}