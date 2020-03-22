package com.github.niuhf0452.exile.web.netty

import com.github.niuhf0452.exile.web.WebServer
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.concurrent.DefaultThreadFactory

class NettyBackend : WebServer.Backend {
    override fun startServer(config: WebServer.Config, handler: WebServer.BackendHandler): AutoCloseable {
        val boss = NioEventLoopGroup()
        val workerCount = Runtime.getRuntime().availableProcessors()
        val worker = NioEventLoopGroup(workerCount, DefaultThreadFactory("netty-worker", Thread.NORM_PRIORITY))
        val bootstrap = ServerBootstrap()
        bootstrap.group(boss, worker)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(HttpServerInitializer())
                .bind(config.port)
        return AutoCloseable {
            boss.shutdownGracefully()
            worker.shutdownGracefully()
        }
    }

    private class HttpServerInitializer : ChannelInitializer<SocketChannel>() {
        override fun initChannel(socketChannel: SocketChannel) {
            val p = socketChannel.pipeline()
        }
    }
}