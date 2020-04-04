package com.github.niuhf0452.exile.web.server

import com.github.niuhf0452.exile.common.PublicApi
import com.github.niuhf0452.exile.web.Router
import com.github.niuhf0452.exile.web.WebServer
import com.github.niuhf0452.exile.web.internal.RouterImpl
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import javax.servlet.Servlet
import kotlin.coroutines.CoroutineContext

class JettyServer(
        private val server: Server,
        override val port: Int,
        override val router: Router
) : WebServer {
    override fun close() {
        server.stop()
    }

    @PublicApi
    class Factory : WebServer.Factory {
        override fun startServer(config: WebServer.Config, coroutineContext: CoroutineContext, router: Router?): WebServer {
            val router0 = router ?: RouterImpl(config)
            val server = Server(config.port)
            val servletContext = ServletContextHandler()
            setMaxHeaderSize(server, config.maxHeaderSize)
            addServlet(servletContext, RouterServlet(router0, coroutineContext))
            servletContext.contextPath = config.contextPath
            servletContext.classLoader = this.javaClass.classLoader
            server.handler = servletContext
            server.start()
            return JettyServer(server, getPort(server), router0)
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