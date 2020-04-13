package com.github.niuhf0452.exile.core.web

import com.github.niuhf0452.exile.core.RouterConfigurator
import com.github.niuhf0452.exile.core.WebServerConfiguration
import com.github.niuhf0452.exile.inject.*
import com.github.niuhf0452.exile.web.*
import com.github.niuhf0452.exile.web.interceptor.CorsInterceptor
import com.github.niuhf0452.exile.web.interceptor.LoggingInterceptor
import com.github.niuhf0452.exile.web.interceptor.ServerContextInterceptor
import kotlin.reflect.KClass

@Inject
class AutowireRouterConfigurator(
        private val injector: Injector
) : RouterConfigurator {
    override fun config(router: Router) {
        val corsConfig = injector.getInstance(WebServerConfiguration.CorsConfiguration::class)
        if (corsConfig.enable) {
            router.addInterceptor(CorsInterceptor(
                    allowedOrigins = corsConfig.allowedOrigins.toSet(),
                    allowedMethods = corsConfig.allowedMethods.toSet(),
                    allowedHeaders = corsConfig.allowedHeaders.toSet(),
                    allowCredentials = corsConfig.allowCredentials,
                    maxAge = corsConfig.maxAge.toJavaDuration(),
                    exposedHeaders = corsConfig.exposedHeaders.toList()
            ))
        }
        val loggingConfig = injector.getInstance(WebServerConfiguration.LoggingConfiguration::class)
        if (loggingConfig.enable) {
            router.addInterceptor(LoggingInterceptor(
                    maxEntityLogSize = loggingConfig.maxEntityLogSize.inBytes().toInt(),
                    blacklist = loggingConfig.blacklist.toSet()
            ))
        }
        val contextConfig = injector.getInstance(WebServerConfiguration.ContextConfiguration::class)
        if (contextConfig.enable) {
            router.addInterceptor(ServerContextInterceptor(
                    headerName = contextConfig.headerName
            ))
        }
        val transformerConfig = injector.getInstance(WebServerConfiguration.TransformerConfiguration::class)
        if (transformerConfig.enable) {
            val transformer = injector.getInstance(WebEntityTransformer::class,
                    listOf(Qualifiers.named(transformerConfig.type)))
            router.setEntityTransformer(transformer)
        }
        val handlerConfig = injector.getInstance(WebServerConfiguration.HandlerConfiguration::class)
        if (handlerConfig.enable) {
            val scanner = injector.getInstance(ClassScanner::class)
            val adapter = InjectorAdapter(injector)
            scanner.findByAnnotation(WebEndpoint::class).forEach { cls ->
                router.addTypeSafeHandler(cls, adapter)
            }
        }
    }

    private class InjectorAdapter(
            private val injector: Injector
    ) : TypeSafeHandlerInjector {
        override fun <A : Any> getInstance(cls: KClass<A>): A {
            return injector.getInstance(cls)
        }
    }
}