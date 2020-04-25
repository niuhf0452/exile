package com.github.niuhf0452.exile.config.source

import com.github.niuhf0452.exile.common.PublicApi
import com.github.niuhf0452.exile.config.Config
import com.github.niuhf0452.exile.config.ConfigException
import com.github.niuhf0452.exile.config.ConfigSourceLoader
import com.github.niuhf0452.exile.config.ConfigValue
import com.github.niuhf0452.exile.config.internal.Util.log
import java.net.URI
import java.util.*

@PublicApi
class FileSource(
        private val location: URI
) : Config.Source {
    override fun load(): Iterable<ConfigValue> {
        log.info("Load config from file: $location")
        val parser = getParser(location)
        val values = mutableListOf<ConfigValue>()
        location.toURL().openStream().use { input ->
            parser.parse(input).forEach { (p, v) ->
                values.add(ConfigValue(location, p, v))
            }
        }
        return values
    }

    override fun toString(): String {
        return "FileSource($location)"
    }

    companion object {
        private val parsers: List<FileParser> = ServiceLoader.load(FileParser::class.java).toList()
        val supportedFileTypes: List<String> = parsers.fold(mutableListOf()) { a, b ->
            a.addAll(b.extNames)
            a
        }

        fun getParser(uri: URI): FileParser {
            val path = uri.toString()
            val i = path.lastIndexOf('.')
            if (i < 0) {
                throw ConfigException("Config file type is not supported: $uri")
            }
            val ext = path.substring(i + 1)
            return parsers.find { ext in it.extNames }
                    ?: throw ConfigException("Config file type is not supported: $uri")
        }
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