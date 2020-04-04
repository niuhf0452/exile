package com.github.niuhf0452.exile.common

import java.net.URLDecoder
import java.net.URLEncoder

object URLHelper {
    fun encodePath(s: String): String {
        return URLEncoder.encode(s, Charsets.UTF_8).replace("+", "%20")
    }

    fun decodePath(s: String): String {
        return URLDecoder.decode(s.replace("+", "%2B"), Charsets.UTF_8)
    }

    fun encodeQueryString(s: String): String {
        return URLEncoder.encode(s, Charsets.UTF_8).replace("+", "%20")
    }

    fun decodeQueryString(s: String): String {
        return URLDecoder.decode(s.replace("+", "%20"), Charsets.UTF_8)
    }

    fun parseQueryString(queryString: String): Map<String, String> {
        if (queryString.isEmpty()) {
            return emptyMap()
        }
        val map = mutableMapOf<String, String>()
        queryString.split('&').forEach { kv ->
            val (k, v) = kv.split('=', limit = 2)
            map[decodeQueryString(k)] = decodeQueryString(v)
        }
        return map
    }
}