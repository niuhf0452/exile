package com.github.niuhf0452.exile.core.internal

import com.github.niuhf0452.exile.common.Orders
import com.github.niuhf0452.exile.config.Config
import com.github.niuhf0452.exile.config.ConfigMapper
import com.github.niuhf0452.exile.config.autoConfigure
import com.github.niuhf0452.exile.core.*
import com.github.niuhf0452.exile.core.config.ConfigurationBinder
import com.github.niuhf0452.exile.inject.Injector
import com.github.niuhf0452.exile.inject.TypeKey
import com.github.niuhf0452.exile.inject.TypeLiteral
import com.github.niuhf0452.exile.inject.autoConfigure
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.allSupertypes

class ApplicationImpl : ThreadSafeComponent(), Application {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val listeners = ConcurrentHashMap.newKeySet<Application.Listener<ApplicationEvent>>()

    override val config: Config = Config.newBuilder().autoConfigure().build()

    override val injector: Injector = run {
        val packages = config.getList("inject.packages")
        if (packages.isEmpty()) {
            throw IllegalStateException("Please set the configuration option inject.packages")
        }
        Injector.builder()
                .autoConfigure(*packages.toTypedArray())
                .addBinder(ConfigurationBinder(ConfigMapper.newMapper(config)))
                .enableStatic { c ->
                    c.bind(Config::class).toInstance(config)
                    c.bind(Injector::class).toProvider { injector }
                }
                .build()
    }

    override val components: List<Component> = injector.getBindings(TypeKey(Component::class))
            .map { it.getInstance() as Component }
            .sortedWith(Orders.comparator())

    override fun safeStart() {
        val listenerType = object : TypeLiteral<Application.Listener<ApplicationEvent>>() {}.typeKey
        injector.getBindings(listenerType).forEach { binding ->
            addListener(binding.getInstance() as Application.Listener<*>)
        }
        components.forEach { component ->
            component.start()
            fireEvent(AfterComponentStart(component))
        }
        log.info("Application started.")
        fireEvent(AfterComponentStart(this))
    }

    override fun safeStop() {
        fireEvent(BeforeComponentStop(this))
        components.asReversed().forEach { component ->
            fireEvent(BeforeComponentStop(component))
            component.stop()
        }
        injector.close()
        log.info("Application stopped.")
    }

    override fun installShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread {
            log.info("Start executing shutdown hook.")
            stop()
        })
    }

    override fun run() {
        installShutdownHook()
        start()
        awaitTerminated()
    }

    override fun addListener(listener: Application.Listener<*>) {
        listeners.add(ListenerAdapter(listener))
    }

    private fun fireEvent(e: ApplicationEvent) {
        listeners.forEach { it.onEvent(e) }
    }

    private class ListenerAdapter(listener: Application.Listener<*>) : Application.Listener<ApplicationEvent> {
        private val eventClass = run {
            val t = listener::class.allSupertypes.find { it.classifier == Application.Listener::class }
                    ?: throw IllegalArgumentException("Can't find the event type: $listener")
            t.arguments[0].type?.classifier as? KClass<*>
                    ?: throw IllegalArgumentException("Can't find the event type: $listener")
        }

        @Suppress("UNCHECKED_CAST")
        private val listener0 = listener as Application.Listener<ApplicationEvent>

        override fun onEvent(event: ApplicationEvent) {
            if (eventClass.isInstance(event)) {
                listener0.onEvent(event)
            }
        }
    }
}