package com.github.niuhf0452.exile.web.servlet

import com.github.niuhf0452.exile.web.*
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class WebServerServlet(
        private val handler: WebServer.BackendHandler
) : HttpServlet() {
    override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
        val asyncContext = req.startAsync()
        handler.onRequest(RequestAdapter(req)).thenAccept { response ->
            response.writeToServletResponse(resp)
            asyncContext.complete()
        }.exceptionally { ex ->
            resp.sendError(500)
            resp.setHeader("Content-Type", "text/plain")
            try {
                ex.printStackTrace(resp.writer)
                resp.flushBuffer()
            } finally {
                resp.writer.close()
                asyncContext.complete()
            }
            null
        }
    }

    private fun WebResponse.writeToServletResponse(servletResponse: HttpServletResponse) {
        servletResponse.status = statusCode
        headers.forEach { (name, values) ->
            values.forEach { value ->
                servletResponse.addHeader(name, value)
            }
        }
        TODO("write entity")
    }

    private class RequestAdapter(servletRequest: HttpServletRequest) : WebRequest {
        override val uri: WebURI = WebURI.parse(servletRequest.requestURI)
        override val headers: WebHeaders = HeaderAdapter(servletRequest)
        override val entity: WebEntityData = EntityAdapter(servletRequest)
    }

    private class HeaderAdapter(
            private val servletRequest: HttpServletRequest
    ) : WebHeaders {
        override fun iterator(): Iterator<Pair<String, Iterable<String>>> {
            return HeadersIterator(servletRequest)
        }
    }

    private class HeadersIterator(
            private val servletRequest: HttpServletRequest
    ) : Iterator<Pair<String, Iterable<String>>> {
        private val namesIterator = servletRequest.headerNames

        override fun hasNext(): Boolean {
            return namesIterator.hasMoreElements()
        }

        override fun next(): Pair<String, Iterable<String>> {
            val name = namesIterator.nextElement()
            return name.toLowerCase() to HeaderValues(servletRequest, name)
        }
    }

    private class HeaderValues(
            private val servletRequest: HttpServletRequest,
            private val name: String
    ) : Iterable<String> {
        override fun iterator(): Iterator<String> {
            return servletRequest.getHeaders(name).asIterator()
        }
    }

    private class EntityAdapter(
            private val servletRequest: HttpServletRequest
    ) : WebEntityData
}