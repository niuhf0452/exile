package com.github.niuhf0452.exile.web.internal

import com.github.niuhf0452.exile.web.CommonHeaders
import com.github.niuhf0452.exile.web.FailureResponseException
import com.github.niuhf0452.exile.web.WebExceptionHandler
import com.github.niuhf0452.exile.web.WebResponse
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.PrintWriter

class DefaultExceptionHandler : WebExceptionHandler {
    private val log = LoggerFactory.getLogger(WebExceptionHandler::class.java)

    override fun handle(exception: Throwable): WebResponse<ByteArray> {
        if (exception is FailureResponseException) {
            return WebResponse.newBuilder()
                    .statusCode(exception.statusCode)
                    .addHeader(CommonHeaders.ContentType, "text/plain")
                    .entity(exception.description.toByteArray())
                    .build()
        }
        val entity = ByteArrayOutputStream().use { out ->
            PrintWriter(out, false, Charsets.UTF_8).use { writer ->
                exception.printStackTrace(writer)
                writer.flush()
            }
            out.toByteArray()
        }
        log.error("Fail to handle web request", exception)
        return WebResponse.newBuilder()
                .statusCode(500)
                .addHeader(CommonHeaders.ContentType, "text/plain")
                .entity(entity)
                .build()
    }
}