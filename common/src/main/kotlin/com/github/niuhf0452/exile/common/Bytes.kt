package com.github.niuhf0452.exile.common

object Bytes {
    private val hexTable = CharArray(16) { i ->
        if (i < 10) {
            '0' + i
        } else {
            'a' + (i - 10)
        }
    }

    fun toHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        bytes.forEach { b ->
            val i = b.toInt() and 0xff
            sb.append(hexTable[i shr 4])
            sb.append(hexTable[i and 0x0f])
        }
        return sb.toString()
    }

    fun fromHex(value: String): ByteArray {
        if (value.length % 2 != 0) {
            throw IllegalArgumentException()
        }
        val s = value.toLowerCase()
        return ByteArray(value.length / 2) { i ->
            val j = i * 2
            ((getDigit(s[j]) shl 4) + getDigit(s[j + 1])).toByte()
        }
    }

    private fun getDigit(c: Char): Int {
        return when (c) {
            in '0'..'9' -> c - '0'
            in 'a'..'f' -> c - 'a' + 10
            else -> throw IllegalArgumentException()
        }
    }
}