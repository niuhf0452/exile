package com.github.niuhf0452.exile.config.impl

import com.github.niuhf0452.exile.config.Config
import org.slf4j.LoggerFactory

object Util {
    val log = LoggerFactory.getLogger(Config::class.java)

    val configPathRegex = "^[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)*$".toRegex()

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
}