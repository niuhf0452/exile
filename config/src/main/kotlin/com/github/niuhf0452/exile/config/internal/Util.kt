package com.github.niuhf0452.exile.config.internal

import com.github.niuhf0452.exile.config.Config
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Util {
    val log: Logger = LoggerFactory.getLogger(Config::class.java)

    val configPathRegex = "^[a-zA-Z0-9_\\-]+(\\.[a-zA-Z0-9_\\-]+)*$".toRegex()
}