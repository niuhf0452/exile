package com.github.niuhf0452.exile.web.server

import com.github.niuhf0452.exile.web.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.await
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
        GlobalScope.launch(context = context, start = CoroutineStart.UNDISPATCHED) {
            try {
                val request = readRequest(asyncContext, req)
                val response = router.onRequest(request)
                response.writeToServletResponse(resp)
            } catch (ex: Exception) {
                ex.writeToServletResponse(resp)
            } finally {
                asyncContext.complete()
            }
        }
    }

    private suspend fun readRequest(asyncContext: AsyncContext, req: HttpServletRequest): WebRequest<ByteArray> {
        val uri = URI.create(req.requestURI)
        val headers = readHeaders(req)
        val bytes = readInput(asyncContext.request.inputStream).await()
        return WebRequest(uri, req.method, headers, if (bytes.isEmpty()) null else bytes)
    }

    private fun readHeaders(servletRequest: HttpServletRequest): MultiValueMap {
        val headers = MultiValueMap(false)
        val e = servletRequest.headerNames
        while (e.hasMoreElements()) {
            val name = e.nextElement()
            val e2 = servletRequest.getHeaders(name)
            while (e2.hasMoreElements()) {
                e2.nextElement().split(',').forEach { value ->
                    val v = value.trim()
                    if (v.isNotEmpty()) {
                        headers.add(name, v)
                    }
                }
            }
        }
        return headers
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
        servletResponse.setHeader(CommonHeaders.ContentType, "text/plain")
        printStackTrace(servletResponse.writer)
    }

    private fun WebResponse<ByteArray>.writeToServletResponse(servletResponse: HttpServletResponse) {
        servletResponse.status = statusCode
        headers.forEach { name ->
            headers.get(name).forEach { value ->
                servletResponse.addHeader(name, value)
            }
        }
        if (entity != null) {
            servletResponse.outputStream.write(entity)
        }
    }
}