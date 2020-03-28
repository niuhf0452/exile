package com.github.niuhf0452.exile.web.server

import com.github.niuhf0452.exile.web.Router
import com.github.niuhf0452.exile.web.WebHeaders
import com.github.niuhf0452.exile.web.WebRequest
import com.github.niuhf0452.exile.web.WebResponse
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.util.concurrent.CompletableFuture
import javax.servlet.AsyncContext
import javax.servlet.ReadListener
import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.coroutines.CoroutineContext

class RouterServlet(
        private val router: Router,
        private val context: CoroutineContext
) : HttpServlet() {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
        val asyncContext = req.startAsync()
        readInput(asyncContext) { bytes ->
            val request = RequestAdapter(URI.create(req.requestURI), req.method,
                    HeaderAdapter(req), bytes)
            GlobalScope.launch(context = context, start = CoroutineStart.UNDISPATCHED) {
                val response = router.onRequest(request)
                try {
                    response.writeToServletResponse(resp)
                } finally {
                    asyncContext.complete()
                }
            }
        }
    }

    private fun readInput(asyncContext: AsyncContext, callback: (ByteArray) -> Unit) {
        readInput(asyncContext.request.inputStream).whenComplete { bytes, ex ->
            if (ex != null) {
                val resp = asyncContext.response as HttpServletResponse
                try {
                    ex.writeToServletResponse(resp)
                } finally {
                    asyncContext.complete()
                }
            } else {
                callback(bytes)
            }
        }
    }

    private fun readInput(input: ServletInputStream): CompletableFuture<ByteArray> {
        val future = CompletableFuture<ByteArray>()
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        input.setReadListener(object : ReadListener {
            override fun onDataAvailable() {
                try {
                    while (input.isReady && !input.isFinished) {
                        val c = input.read(buffer)
                        if (c > 0) {
                            out.write(buffer, 0, c)
                        }
                    }
                } catch (ex: IOException) {
                    future.completeExceptionally(ex)
                }
            }

            override fun onAllDataRead() {
                future.complete(out.toByteArray())
            }

            override fun onError(t: Throwable) {
                future.completeExceptionally(t)
            }
        })
        return future
    }

    private fun Throwable.writeToServletResponse(servletResponse: HttpServletResponse) {
        servletResponse.sendError(500)
        servletResponse.setHeader("Content-Type", "text/plain")
        printStackTrace(servletResponse.writer)
    }

    private fun WebResponse<ByteArray>.writeToServletResponse(servletResponse: HttpServletResponse) {
        servletResponse.status = statusCode
        headers.forEach { name ->
            headers.get(name).forEach { value ->
                servletResponse.addHeader(name, value)
            }
        }
        servletResponse.outputStream.write(entity)
        servletResponse.outputStream.close()
    }

    private class RequestAdapter(
            override val uri: URI,
            override val method: String,
            override val headers: WebHeaders,
            override val entity: ByteArray
    ) : WebRequest<ByteArray>

    private class HeaderAdapter(
            private val servletRequest: HttpServletRequest
    ) : WebHeaders {
        override fun get(name: String): Iterable<String> {
            return HeaderValues(servletRequest, name)
        }

        override fun iterator(): Iterator<String> {
            return servletRequest.headerNames.asIterator()
        }
    }

    private class HeaderValues(
            private val servletRequest: HttpServletRequest,
            private val name: String
    ) : Iterable<String> {
        override fun iterator(): Iterator<String> {
            return servletRequest.getHeaders(name)?.asIterator()
                    ?: emptyList<String>().iterator()
        }
    }
}