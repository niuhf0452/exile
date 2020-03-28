package com.github.niuhf0452.exile.web.server

import com.github.niuhf0452.exile.web.Router
import com.github.niuhf0452.exile.web.SerialModuleElement
import com.github.niuhf0452.exile.web.WebServer
import com.github.niuhf0452.exile.web.impl.RouterImpl
import kotlinx.serialization.modules.EmptyModule
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import javax.servlet.Servlet
import kotlin.coroutines.CoroutineContext

class JettyServer(
        router: Router,
        private val server: Server,
        override val port: Int
) : WebServer, Router by router {
    override fun close() {
        server.stop()
    }

    class Factory : WebServer.Factory {
        override fun startServer(config: WebServer.Config, coroutineContext: CoroutineContext): WebServer {
            val module = coroutineContext[SerialModuleElement]
                    ?.module
                    ?: EmptyModule
            val router = RouterImpl(config, module)
            val server = Server(config.port)
            val servletContext = ServletContextHandler()
            setMaxHeaderSize(server, config.maxHeaderSize)
            addServlet(servletContext, RouterServlet(router, coroutineContext))
            servletContext.contextPath = config.contextPath
            servletContext.classLoader = this.javaClass.classLoader
            server.handler = servletContext
            server.start()
            return JettyServer(router, server, getPort(server))
        }

        private fun setMaxHeaderSize(server: Server, maxHeaderSize: Int) {
            for (c in server.connectors) {
                val cf = c.getConnectionFactory(HttpConnectionFactory::class.java)
                if (cf != null) {
                    cf.httpConfiguration.requestHeaderSize = maxHeaderSize
                }
            }
        }

        private fun addServlet(context: ServletContextHandler, servlet: Servlet) {
            val holder = ServletHolder(servlet)
            holder.isAsyncSupported = true
            context.addServlet(holder, "/*")
        }

        private fun getPort(server: Server): Int {
            for (c in server.connectors) {
                if (c is ServerConnector) {
                    return c.localPort
                }
            }
            throw IllegalStateException()
        }
    }
}