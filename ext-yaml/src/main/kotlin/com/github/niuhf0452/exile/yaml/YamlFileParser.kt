package com.github.niuhf0452.exile.yaml

import com.charleskorn.kaml.*
import com.github.niuhf0452.exile.config.source.FileParser
import java.io.InputStream

class YamlFileParser : FileParser {
    override val extNames: List<String> = listOf("yaml", "yml")

    override fun parse(input: InputStream): Iterable<Pair<String, String>> {
        val parser = YamlParser(input.readAllBytes().toString(Charsets.UTF_8))
        val reader = YamlNodeReader(parser)
        val rootNode = reader.read()
        parser.ensureEndOfStreamReached()
        when (rootNode) {
            is YamlList, is YamlMap -> Unit
            else -> throw IllegalArgumentException("The root of YAML should be list or map")
        }
        val out = mutableListOf<Pair<String, String>>()
        parse(out, "", rootNode)
        return out
    }

    private fun parse(out: MutableList<Pair<String, String>>, path: String, node: YamlNode) {
        when (node) {
            is YamlScalar -> out.add(path to node.content)
            is YamlNull -> Unit
            is YamlList -> {
                node.items.forEachIndexed { i, n ->
                    val p = if (path.isEmpty()) i.toString() else "$path.$i"
                    parse(out, p, n)
                }
            }
            is YamlMap -> {
                node.entries.forEach { (k, v) ->
                    if (k !is YamlScalar) {
                        throw IllegalArgumentException("Unsupported key type: $k")
                    }
                    val p = if (path.isEmpty()) k.content else "$path.${k.content}"
                    parse(out, p, v)
                }
            }
            else -> throw IllegalStateException()
        }
    }
}