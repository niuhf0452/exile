package com.github.niuhf0452.exile.config.source

import com.github.niuhf0452.exile.common.PublicApi
import com.github.niuhf0452.exile.config.Config
import com.github.niuhf0452.exile.config.ConfigException
import com.github.niuhf0452.exile.config.ConfigSourceLoader
import com.github.niuhf0452.exile.config.ConfigValue
import com.github.niuhf0452.exile.config.internal.Util.log
import com.github.niuhf0452.exile.config.simpleconfig.SimpleConfigParser
import java.io.InputStream
import java.net.URI
import java.util.*

@PublicApi
class FileSource(
        private val location: URI,
        private val fileType: FileType = getFileType(location)
) : Config.Source {
    override fun load(): Iterable<ConfigValue> {
        log.info("Load config from file: $fileType, $location")
        val values = mutableListOf<ConfigValue>()
        location.toURL().openStream().use { input ->
            fileType.parse(input).forEach { (p, v) ->
                values.add(ConfigValue(location, p, v))
            }
        }
        return values
    }

    override fun toString(): String {
        return "FileSource($location)"
    }

    companion object {
        val supportedFileTypes = listOf(FileType.CONF, FileType.PROPERTIES)

        fun getFileType(uri: URI): FileType {
            val path = uri.path
            return supportedFileTypes.find { ft ->
                path.endsWith("." + ft.ext)
            } ?: throw ConfigException("Config file type is not supported: $uri")
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

    class Loader : ConfigSourceLoader {
        override fun load(uri: URI): Config.Source? {
            if (uri.scheme == "file" || uri.scheme == "http") {
                return try {
                    FileSource(uri)
                } catch (ex: Exception) {
                    null
                }
            }
            return null
        }
    }
}