package com.github.niuhf0452.exile.core.web

import com.github.niuhf0452.exile.core.RouterConfigurator
import com.github.niuhf0452.exile.core.ThreadSafeComponent
import com.github.niuhf0452.exile.core.WebServerConfiguration
import com.github.niuhf0452.exile.inject.*
import com.github.niuhf0452.exile.web.WebServer
import com.github.niuhf0452.exile.web.internal.RouterImpl
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

@Inject
class WebServerComponent(
        private val config: WebServerConfiguration,
        private val injector: Injector
) : ThreadSafeComponent() {
    private var server: WebServer? = null

    override fun safeStart() {
        val factory = injector.getInstance(WebServer.Factory::class, listOf(Qualifiers.named(config.type)))
        val conf = WebServer.Config(
                host = config.host,
                port = config.port,
                contextPath = config.contextPath,
                maxRequestLineSize = config.maxRequestLineSize.inBytes().toInt(),
                maxHeaderSize = config.maxHeaderSize.inBytes().toInt(),
                maxEntitySize = config.maxEntitySize.inBytes().toInt(),
                keepAlive = config.keepAlive,
                serverHeader = config.serverHeader
        )
        val dispatcher = Executors.newFixedThreadPool(config.threadSize, WebThreadGroup())
                .asCoroutineDispatcher()
        val router = RouterImpl(conf)
        injector.getBindings(TypeKey(RouterConfigurator::class)).forEach { binding ->
            val configurator = binding.getInstance() as RouterConfigurator
            configurator.config(router)
        }
        server = factory.startServer(conf, dispatcher, router)
    }

    override fun safeStop() {
        server?.close()
    }

    private class WebThreadGroup : ThreadFactory {
        private val group = ThreadGroup("web-server")
        private val counter = AtomicInteger(0)

        override fun newThread(r: Runnable): Thread {
            val thread = Thread(group, r)
            thread.isDaemon = false
            thread.name = "web-server-worker-${counter.getAndIncrement()}"
            return thread
        }
    }
}
