package com.github.niuhf0452.exile.web

val emptyByteArray = ByteArray(0)

object Responses {
    val NotFound = status(404)
    val NoContent = status(204)
    val NotAcceptable = status(406)
    val UnsupportedMediaType = status(415)

    private fun status(value: Int): WebResponse<ByteArray> = StatusResponse(value)

    private class StatusResponse(override val statusCode: Int) : WebResponse<ByteArray> {
        override val headers: WebHeaders
            get() = WebHeaders.Empty
        override val entity: ByteArray
            get() = emptyByteArray
    }
}