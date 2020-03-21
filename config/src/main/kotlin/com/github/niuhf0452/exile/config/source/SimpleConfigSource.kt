package com.github.niuhf0452.exile.config.source

import com.github.niuhf0452.exile.config.Config
import com.github.niuhf0452.exile.config.ConfigSourceLoader
import com.github.niuhf0452.exile.config.ConfigValue
import com.github.niuhf0452.exile.config.impl.Util
import com.github.niuhf0452.exile.config.impl.Util.log
import com.github.niuhf0452.exile.config.impl.Util.toHexString
import com.github.niuhf0452.exile.config.simpleconfig.SimpleConfigParser
import java.net.URI
import java.security.MessageDigest

class SimpleConfigSource(
        private val content: String
) : Config.Source {
    private val hash = MessageDigest
            .getInstance("SHA1")
            .digest(content.toByteArray())
            .toHexString()

    override fun load(): Iterable<ConfigValue> {
        log.info("Load config from simple config:\n$content\n")
        val parser = SimpleConfigParser.newParser(content)
        return parser.parse().map { (p, v) -> ConfigValue(location, p, v) }
    }

    override fun toString(): String {
        return "SimpleConfigSource($hash)"
    }

    class Loader : ConfigSourceLoader {
        override fun load(uri: URI): Config.Source? {
            if (uri.scheme == location.scheme) {
                val qs = uri.rawQuery
                        ?.let(Util::parseQueryString)
                        ?: emptyMap()
                val content = qs["content"] ?: ""
                return SimpleConfigSource(content)
            }
            return null
        }
    }

    companion object {
        val location: URI = URI.create("simple://mem")
    }
}
