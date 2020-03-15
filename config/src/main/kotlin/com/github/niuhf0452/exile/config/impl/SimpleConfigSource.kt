package com.github.niuhf0452.exile.config.impl

import com.github.niuhf0452.exile.config.Config
import com.github.niuhf0452.exile.config.ConfigValue
import com.github.niuhf0452.exile.config.impl.Util.log
import com.github.niuhf0452.exile.config.impl.Util.toHexString
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
        return parser.parse().map { (p, v) -> ConfigValue(this, p, v) }
    }

    override fun toString(): String {
        return "SimpleConfigSource($hash)"
    }
}
