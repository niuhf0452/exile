package com.github.niuhf0452.exile.core

import com.github.niuhf0452.exile.common.Orders
import com.github.niuhf0452.exile.config.Config
import com.github.niuhf0452.exile.config.autoConfigure
import com.github.niuhf0452.exile.inject.Injector
import com.github.niuhf0452.exile.inject.TypeKey
import com.github.niuhf0452.exile.inject.autoConfigure

class Application : ThreadSafeComponent() {
    private var context: AppContext? = null

    override fun safeStart() {
        val config = Config.newBuilder().autoConfigure().build()
        val packages = config.getList("exile.inject.packages")
        val injector = Injector.builder()
                .autoConfigure(*packages.toTypedArray())
                .enableStatic { c ->
                    c.bind(Config::class).toInstance(config)
                }
                .build()
        val components = injector.getBindings(TypeKey(Component::class))
                .map { it.getInstance() as Component }
                .sortedWith(Orders.comparator())
        context = AppContext(config, injector, components)
        components.forEach { it.start() }
    }

    override fun safeStop() {
        val c = context
        if (c != null) {
            context = null
            c.components.asReversed().forEach { it.stop() }
            c.injector.close()
        }
    }

    fun installShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread(::stop))
    }

    private class AppContext(
            val config: Config,
            val injector: Injector,
            val components: List<Component>
    )

    companion object {
        fun run() {
            val app = Application()
            app.installShutdownHook()
            app.start()
            app.awaitTerminal()
        }
    }
}