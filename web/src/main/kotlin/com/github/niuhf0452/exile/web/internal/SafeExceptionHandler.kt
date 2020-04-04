package com.github.niuhf0452.exile.web.internal

import com.github.niuhf0452.exile.web.WebExceptionHandler
import com.github.niuhf0452.exile.web.WebResponse

class SafeExceptionHandler(
        private val handler: WebExceptionHandler
) : WebExceptionHandler {
    private val fallbackHandler = DefaultExceptionHandler()

    override fun handle(exception: Throwable): WebResponse<ByteArray> {
        return try {
            handler.handle(exception)
        } catch (ex: Throwable) {
            fallbackHandler.handle(ex)
        }
    }
}