package com.github.niuhf0452.exile.config.impl

import com.github.niuhf0452.exile.config.ConfigException
import java.io.BufferedReader
import java.io.StringReader
import java.util.*
import kotlin.collections.ArrayList

class SimpleConfigParser(
        private val reader: Reader
) {
    private val backtrace = Stack<String>()
    private val values = mutableListOf<Pair<String, String>>()
    private val nameRegex = "[a-zA-Z0-9\\-_]+(\\.[a-zA-Z0-9\\-_]+)*".toRegex()

    fun parse(): Iterable<Pair<String, String>> {
        backtrace.clear()
        values.clear()
        try {
            while (true) {
                skipWhitespace()
                when (reader.read()) {
                    '}' -> {
                        skipWhitespace()
                        skipEOL()
                        closeBlock()
                    }
                    '\n' -> Unit
                    else -> {
                        reader.rewind()
                        parseNamedElement()
                    }
                }
            }
        } catch (ex: EOFException) {
            if (backtrace.isNotEmpty()) {
                throwError("Missing '}'")
            }
            return ArrayList(values)
        }
    }

    private fun closeBlock() {
        if (backtrace.isEmpty()) {
            throwError("The '}' doesn't match '{'")
        }
        backtrace.pop()
    }

    private fun getPath(name: String): String {
        return if (backtrace.isEmpty()) {
            name
        } else {
            "${backtrace.peek()}.$name"
        }
    }

    private fun parseNamedElement() {
        val name = parseName()
        skipWhitespace()
        when (reader.read()) {
            '=' -> {
                skipWhitespace()
                val value = parseValue().trim()
                values.add(getPath(name) to value)
            }
            '{' -> {
                skipWhitespace()
                skipEOL()
                backtrace.push(getPath(name))
            }
            else -> throwError("Expect '=' or '{'")
        }
    }

    private fun throwError(message: String): Nothing {
        throw ConfigException("Fail to parse config at " +
                "line ${reader.line}, col ${reader.col} : $message")
    }

    private fun parseName(): String {
        val sb = StringBuilder()
        while (true) {
            when (val c = reader.read()) {
                in '0'..'9',
                in 'a'..'z',
                in 'A'..'Z',
                '-', '_', '.' -> sb.append(c)
                ' ', '{', '=' -> {
                    reader.rewind()
                    val name = sb.toString()
                    if (!nameRegex.matches(name)) {
                        throwError("name invalid: $name")
                    }
                    return name
                }
                else -> throwError("Invalid character: $c")
            }
        }
    }

    private fun parseValue(): String {
        try {
            val sb = StringBuilder()
            while (true) {
                val c = reader.read()
                if (c == '\n') {
                    return sb.toString().trim()
                }
                sb.append(c)
            }
        } catch (ex: EOFException) {
            throwError("Unexpected EOF")
        }
    }

    private fun skipWhitespace() {
        while (true) {
            val c = reader.read()
            if (c != ' ' && c != '\t') {
                reader.rewind()
                break
            }
        }
    }

    private fun skipEOL() {
        if (reader.read() != '\n') {
            throwError("Expect end of line")
        }
    }

    class EOFException : RuntimeException()

    companion object {
        fun newParser(input: String): SimpleConfigParser {
            return SimpleConfigParser(ReaderImpl(input))
        }
    }

    interface Reader {
        val line: Int
        val col: Int

        @Throws(EOFException::class)
        fun read(): Char

        fun rewind()
    }

    class ReaderImpl(content: String) : Reader {
        private val input = BufferedReader(StringReader(content + "\n"))
        private val buffer = CharArray(4096)
        private var offset = 0
        private var end = 0

        override var line: Int = 1
        override var col: Int = 0

        override fun read(): Char {
            if (offset >= end) {
                if (offset >= buffer.size) {
                    offset = 0
                }
                val c = input.read(buffer, offset, buffer.size - offset)
                if (c == -1) {
                    throw EOFException()
                }
                end = offset + c
            }
            val char = buffer[offset++]
            if (char == '\n') {
                line++
                col = 0
            } else {
                col++
            }
            return char
        }

        override fun rewind() {
            offset--
        }
    }
}