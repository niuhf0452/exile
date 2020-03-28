package com.github.niuhf0452.exile.web.impl

import com.github.niuhf0452.exile.web.WebExceptionHandler
import com.github.niuhf0452.exile.web.WebResponse
import com.github.niuhf0452.exile.web.WebResponseException
import java.io.ByteArrayOutputStream
import java.io.PrintWriter

class DefaultExceptionHandler : WebExceptionHandler {
    override fun handle(exception: Exception): WebResponse<ByteArray> {
        if (exception is WebResponseException) {
            return exception.response
        }
        val entity = ByteArrayOutputStream().use { out ->
            PrintWriter(out, false, Charsets.UTF_8).use { writer ->
                exception.printStackTrace(writer)
                writer.flush()
            }
            out.toByteArray()
        }
        return WebResponse.newBuilder()
                .statusCode(500)
                .addHeader("Content-Type", "text/plain")
                .entity(entity)
    }
}