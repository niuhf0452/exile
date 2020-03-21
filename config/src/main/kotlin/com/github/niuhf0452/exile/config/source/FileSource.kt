package com.github.niuhf0452.exile.config.source

import com.github.niuhf0452.exile.config.Config
import com.github.niuhf0452.exile.config.ConfigException
import com.github.niuhf0452.exile.config.ConfigValue
import com.github.niuhf0452.exile.config.impl.Util.log
import com.github.niuhf0452.exile.config.impl.SimpleConfigParser
import java.io.InputStream
import java.net.URL
import java.util.*

class FileSource(
        private val url: URL,
        private val fileType: FileType = getFileType(url)
) : Config.Source {
    override fun load(): Iterable<ConfigValue> {
        log.info("Load config from file: $fileType, $url")
        val values = mutableListOf<ConfigValue>()
        url.openStream().use { input ->
            fileType.parse(input).forEach { (p, v) ->
                values.add(ConfigValue(this, p, v))
            }
        }
        return values
    }

    override fun toString(): String {
        return "FileSource($url)"
    }

    companion object {
        val supportedFileTypes = listOf(FileType.CONF, FileType.PROPERTIES)

        fun getFileType(url: URL): FileType {
            val path = url.path
            return supportedFileTypes.find { ft ->
                path.endsWith("." + ft.ext)
            } ?: throw ConfigException("Config file type is not supported: $url")
        }
    }

    interface Parser {
        fun parse(input: InputStream): Iterable<Pair<String, String>>
    }

    enum class FileType : Parser {
        CONF {
            override val ext: String
                get() = "conf"

            override fun parse(input: InputStream): Iterable<Pair<String, String>> {
                val text = input.readAllBytes().toString(Charsets.UTF_8)
                val parser = SimpleConfigParser.newParser(text)
                return parser.parse()
            }
        },
        PROPERTIES {
            override val ext: String
                get() = "properties"

            override fun parse(input: InputStream): Iterable<Pair<String, String>> {
                val props = Properties()
                props.load(input)
                return props.map { (k, v) ->
                    k.toString() to v.toString()
                }
            }
        };

        abstract val ext: String
    }
}