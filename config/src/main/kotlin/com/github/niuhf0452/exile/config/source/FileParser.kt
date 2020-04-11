package com.github.niuhf0452.exile.config.source

import com.github.niuhf0452.exile.config.simpleconfig.SimpleConfigParser
import java.io.InputStream
import java.util.*

interface FileParser {
    val extNames: List<String>

    fun parse(input: InputStream): Iterable<Pair<String, String>>
}

class ConfFileParser : FileParser {
    override val extNames: List<String> = listOf("conf")

    override fun parse(input: InputStream): Iterable<Pair<String, String>> {
        val text = input.readAllBytes().toString(Charsets.UTF_8)
        val parser = SimpleConfigParser.newParser(text)
        return parser.parse()
    }
}

class PropertiesFileParser : FileParser {
    override val extNames: List<String> = listOf("properties")

    override fun parse(input: InputStream): Iterable<Pair<String, String>> {
        val props = Properties()
        props.load(input)
        return props.map { (k, v) ->
            k.toString() to v.toString()
        }
    }
}