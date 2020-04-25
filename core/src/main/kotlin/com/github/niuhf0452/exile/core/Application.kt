package com.github.niuhf0452.exile.core

import com.github.niuhf0452.exile.common.DataSize
import com.github.niuhf0452.exile.common.Duration
import com.github.niuhf0452.exile.common.StringList
import com.github.niuhf0452.exile.config.Config
import com.github.niuhf0452.exile.config.Configuration
import com.github.niuhf0452.exile.core.internal.ApplicationImpl
import com.github.niuhf0452.exile.inject.Injector
import com.github.niuhf0452.exile.web.Router
import kotlinx.serialization.Serializable

/**
 * The entry point of application.
 *
 * @since 1.0
 */
interface Application : Component {
    /**
     * The [Config] instance.
     */
    val config: Config

    /**
     * The [Injector] instance.
     */
    val injector: Injector

    /**
     * The list of [Component] loaded.
     */
    val components: List<Component>

    /**
     * Install a shutdown hook which will call stop to shutdown the application.
     */
    fun installShutdownHook()

    /**
     * Start the application and block current thread until the application stopped.
     */
    fun run()

    /**
     * Add a listener of [ApplicationEvent]. The listener will be called in a thread-safe manner.
     * That means even multiple events occurs in the same time, the listener will be called for each event
     * in a single thread without concurrent.
     *
     * Note that it's not a event bus, the performance is not guaranteed for large amount of events.
     *
     * @param listener The listener.
     */
    fun addListener(listener: Listener<*>)

    interface Listener<E : ApplicationEvent> {
        fun onEvent(event: E)
    }

    companion object {
        /**
         * Create an instance of [Application].
         */
        operator fun invoke(): Application {
            return ApplicationImpl()
        }
    }
}

/**
 * Component is a class with lifecycle tied to the application.
 *
 * An instance of Component can only be started once.
 *
 * @since 1.0
 */
interface Component {
    /**
     * Start a component.
     *
     * This method shouldn't throw exception if the component has already started.
     */
    fun start()

    /**
     * Stop a component.
     *
     * This method should never throw exception. If the component has already stopped,
     * the call of the method should do nothing.
     */
    fun stop()

    /**
     * Wait for terminating.
     */
    fun awaitTerminated()
}

/**
 * An event to reflect state change of application.
 * Generally this type of event is used to interact with the systems outside of application.
 */
interface ApplicationEvent

/**
 * Be fired after component started.
 */
data class AfterComponentStart(val component: Component) : ApplicationEvent

/**
 * Be fired before component stops.
 */
data class BeforeComponentStop(val component: Component) : ApplicationEvent

/**
 * A SPI for extending usage of router.
 */
interface RouterConfigurator {
    fun config(router: Router)
}

/**
 * The configuration options of web server.
 */
@Serializable
@Configuration("web.server")
data class WebServerConfiguration(
        val type: String,
        val host: String,
        val port: Int,
        val contextPath: String,
        val maxRequestLineSize: DataSize,
        val maxHeaderSize: DataSize,
        val maxEntitySize: DataSize,
        val keepAlive: Boolean,
        val serverHeader: String,
        val threadSize: Int,
        val cors: CorsConfiguration,
        val logging: LoggingConfiguration,
        val context: ContextConfiguration,
        val transformer: TransformerConfiguration,
        val handler: HandlerConfiguration
) {
    @Serializable
    data class CorsConfiguration(
            val enable: Boolean,
            val allowedOrigins: StringList,
            val allowedMethods: StringList,
            val allowedHeaders: StringList,
            val allowCredentials: Boolean,
            val maxAge: Duration,
            val exposedHeaders: StringList
    )

    @Serializable
    data class LoggingConfiguration(
            val enable: Boolean,
            val maxEntityLogSize: DataSize,
            val blacklist: StringList
    )

    @Serializable
    data class ContextConfiguration(
            val enable: Boolean,
            val headerName: String
    )

    @Serializable
    data class TransformerConfiguration(
            val enable: Boolean,
            val type: String
    )

    @Serializable
    data class HandlerConfiguration(
            val enable: Boolean
    )
}