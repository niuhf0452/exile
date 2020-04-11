package com.github.niuhf0452.exile.core.internal

import com.github.niuhf0452.exile.config.Configuration
import com.github.niuhf0452.exile.core.Component
import kotlinx.serialization.Serializable

class WebServerComponent(
        private val config: WebServerConfiguration
) : Component {
    override fun start() {
        config.type
    }

    override fun stop() {

    }
}

@Serializable
@Configuration("web.server")
data class WebServerConfiguration(
        val type: String
)