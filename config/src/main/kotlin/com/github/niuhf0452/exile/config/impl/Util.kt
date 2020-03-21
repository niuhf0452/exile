package com.github.niuhf0452.exile.config.impl

import com.github.niuhf0452.exile.config.Config
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URLDecoder

object Util {
    val log: Logger = LoggerFactory.getLogger(Config::class.java)

    val configPathRegex = "^[a-zA-Z0-9_\\-]+(\\.[a-zA-Z0-9_\\-]+)*$".toRegex()

    private val hexTable = CharArray(16) { i ->
        if (i < 10) {
            '0' + i
        } else {
            'a' + (i - 10)
        }
    }

    fun ByteArray.toHexString(): String {
        val sb = StringBuilder()
        forEach { b ->
            val i = b.toInt() and 0xff
            sb.append(hexTable[i shr 4])
            sb.append(hexTable[i and 0x0f])
        }
        return sb.toString()
    }

    fun getActiveProfiles(): List<String> {
        val active = System.getProperty("config.profiles.active")
                ?: System.getenv("CONFIG_PROFILES_ACTIVE")
        if (active == null || active.isBlank()) {
            return emptyList()
        }
        return active.split(',').map(String::trim)
    }

    fun getConfigFile(): String {
        return System.getProperty("config.file")
                ?: System.getenv("CONFIG_FILE")
                ?: "/application.*"
    }

    fun parseQueryString(queryString: String): Map<String, String> {
        if (queryString.isEmpty()) {
            return emptyMap()
        }
        val map = mutableMapOf<String, String>()
        queryString.split('&').forEach { kv ->
            val (k, v) = kv.split('=', limit = 2)
            map[URLDecoder.decode(k, Charsets.UTF_8)] = URLDecoder.decode(v, Charsets.UTF_8)
        }
        return map
    }
}