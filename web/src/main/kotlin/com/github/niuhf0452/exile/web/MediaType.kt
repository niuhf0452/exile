package com.github.niuhf0452.exile.web

import java.nio.charset.Charset
import java.nio.charset.UnsupportedCharsetException

data class MediaType(
        val type: String,
        val subtype: String,
        val tree: String? = null,
        val suffix: String? = null,
        val charset: Charset = Charsets.UTF_8
) {
    fun isAcceptable(m: MediaType): Boolean {
        return (type == "*" || type == m.type)
                && (subtype == "*" || subtype == m.subtype)
                && (subtype == "*" || tree == m.tree)
                && (subtype == "*" || suffix == m.suffix)
    }

    companion object {
        private val TYPE = "([\\w\\-]+|\\*)/(?:(\\w+)\\.)?([\\w\\-]+|\\*)(?:\\+([\\w\\-]+))?".toPattern()
        private val CHARSET = ";\\s*charset\\s*=\\s*([\\w\\-]+)".toRegex()

        val ALL = parse0("*/*")
        val APPLICATION_JSON = parse0("application/json")
        val APPLICATION_XML = parse0("application/xml")
        val APPLICATION_X_YAML = parse0("application/x-yaml")
        val TEXT_PLAIN = parse0("text/plain")
        val TEXT_HTML = parse0("text/html")
        val TEXT_YAML = parse0("text/yaml")

        private val cached = listOf(
                APPLICATION_JSON,
                APPLICATION_XML,
                APPLICATION_X_YAML,
                TEXT_PLAIN,
                TEXT_HTML,
                TEXT_YAML
        )

        /**
         * Cache for speed.
         */
        private val cacheMap = cached.map { m ->
            "${m.type}/${m.subtype}" to m
        }.toMap()

        fun parse(value: String): MediaType {
            return cacheMap[value]
                    ?: parse0(value)
        }

        private fun parse0(value: String): MediaType {
            if (value.isEmpty()) {
                throw IllegalArgumentException()
            }
            val m = TYPE.matcher(value)
            if (!m.lookingAt()) {
                throw IllegalArgumentException("Invalid media type: $value")
            }
            val type = m.group(1)
            val tree = m.group(2)
            val subtype = m.group(3)
            val suffix = m.group(4)
            val charset = getCharset(value, m.end())
            return MediaType(type, subtype, tree, suffix, charset)
        }

        private fun getCharset(value: String, start: Int): Charset {
            val m = CHARSET.find(value, start)
                    ?: return Charsets.UTF_8
            try {
                return Charset.forName(m.groupValues[1])
            } catch (ex: UnsupportedCharsetException) {
                throw IllegalArgumentException("Invalid media type, unsupported charset: $value")
            }
        }
    }
}