package com.github.niuhf0452.exile.web.servlet

import com.github.niuhf0452.exile.web.WebServer
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import javax.servlet.Servlet

class JettyBackend : WebServer.Backend {
    override fun startServer(config: WebServer.Config, handler: WebServer.BackendHandler): AutoCloseable {
        val server = Server(config.port)
        val servletContext = ServletContextHandler()
        setMaxHeaderSize(server, config.maxHeaderSizeInKB * 1024)
        addServlet(servletContext, WebServerServlet(handler))
        servletContext.contextPath = config.contextPath
        servletContext.classLoader = this.javaClass.classLoader
        server.handler = servletContext
        server.start()
        return AutoCloseable { server.stop() }
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
}