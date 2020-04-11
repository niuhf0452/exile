package com.github.niuhf0452.exile.core

interface Component {
    @Throws(StopException::class)
    fun start()

    fun stop()

    object StopException : RuntimeException("Request stop before started.")
}