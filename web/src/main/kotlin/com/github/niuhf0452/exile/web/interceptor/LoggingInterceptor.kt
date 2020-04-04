package com.github.niuhf0452.exile.web.interceptor

import com.github.niuhf0452.exile.web.*
import org.slf4j.LoggerFactory

class LoggingInterceptor(
        private val maxEntityLogSize: Int,
        private val blacklist: Set<String>
) : WebInterceptor {
    private val log = LoggerFactory.getLogger(this::class.java)

    override val order: Int
        get() = -500

    override suspend fun onRequest(request: WebRequest<ByteArray>, handler: WebInterceptor.RequestHandler): WebResponse<ByteArray> {
        val domain = getTargetDomain(request)
        if (blacklist.contains(domain)) {
            return handler.onRequest(request)
        }
        val sb = StringBuilder()
        sb.append("HTTP request\n")
        logRequest(sb, request)
        log.info(sb.toString())
        sb.setLength(0)
        val response = handler.onRequest(request)
        sb.append("HTTP response\n")
        logResponse(sb, response)
        log.info(sb.toString())
        return response
    }

    private fun getTargetDomain(request: WebRequest<ByteArray>): String {
        return request.headers.get(CommonHeaders.Host).firstOrNull()
                ?: request.uri.authority
                ?: ""
    }

    private fun logRequest(sb: StringBuilder, request: WebRequest<ByteArray>) {
        // request line
        sb.append(prefix).append(request.method)
                .append(" ").append(request.uri)
                .append(" HTTP/1.1")
                .append("\n")
        // headers
        logHeaders(sb, request.headers)
        sb.append(prefix).append("\n")
        // entity
        logEntity(sb, request.entity, request.headers)
    }

    private fun logResponse(sb: StringBuilder, response: WebResponse<ByteArray>) {
        // request line
        sb.append(prefix).append(response.statusCode)
                .append(" ").append(defaultMessages[response.statusCode] ?: "")
                .append("\n")
        // headers
        logHeaders(sb, response.headers)
        sb.append(prefix).append("\n")
        // entity
        logEntity(sb, response.entity, response.headers)
    }

    private fun logHeaders(sb: StringBuilder, headers: MultiValueMap) {
        headers.forEach { name ->
            sb.append(prefix).append(name).append(": ")
            headers.get(name).joinTo(sb, ", ")
            sb.append("\n")
        }
    }

    private fun logEntity(sb: StringBuilder, entity: ByteArray?, headers: MultiValueMap) {
        if (entity != null) {
            val contentType = headers.get(CommonHeaders.ContentType)
                    .firstOrNull()?.let { MediaType.parse(it) }
            when {
                contentType == null -> sb.append("<content type not found>")
                contentType.subtype == "json" || contentType.type == "text" -> {
                    val text = entity.toString(contentType.charset)
                    if (text.length > maxEntityLogSize) {
                        sb.append(text.substring(0, maxEntityLogSize))
                        sb.append("\n...more...")
                    } else {
                        sb.append(text)
                    }
                }
                else -> sb.append("<content type is not text based: $contentType>")
            }
            sb.append("\n")
        }
    }

    companion object {
        private const val prefix = ""

        private val defaultMessages = mapOf(
                100 to "Continue", // The server has received the request headers, and the client should proceed to send the request body
                101 to "Switching Protocols", // The requester has asked the server to switch protocols
                102 to "Processing", // A WebDAV request may contain many sub-requests involving file operations, requiring a long time to complete the request.
                103 to "Early Hints", // Used to return some response headers before final HTTP message
                200 to "OK", // The request is OK (this is the standard response for successful HTTP requests)
                201 to "Created", // The request has been fulfilled, and a new resource is created
                202 to "Accepted", // The request has been accepted for processing, but the processing has not been completed
                203 to "Non-Authoritative Information", // The request has been successfully processed, but is returning information that may be from another source
                204 to "No Content", // The request has been successfully processed, but is not returning any content
                205 to "Reset Content", // The request has been successfully processed, but is not returning any content, and requires that the requester reset the document view
                206 to "Partial Content", // The server is delivering only part of the resource due to a range header sent by the client
                207 to "Multi-Status", // WebDAV
                208 to "Already Reported", // WebDAV
                226 to "IM Used", // RFC 3229
                300 to "Multiple Choices", // A link list. The user can select a link and go to that location. Maximum five addresses
                301 to "Moved Permanently", // The requested page has moved to a new URL
                302 to "Found", // The requested page has moved temporarily to a new URL
                303 to "See Other", // The requested page can be found under a different URL
                304 to "Not Modified", //Indicates the requested page has not been modified since last requested
                305 to "Use Proxy", // The requested resource is available only through a proxy, the address for which is provided in the response
                306 to "Switch Proxy", // No longer used
                307 to "Temporary Redirect", // The requested page has moved temporarily to a new URL
                308 to "Permanent Redirect", // RFC 7538
                400 to "Bad Request", // The request cannot be fulfilled due to bad syntax
                401 to "Unauthorized", // The request was a legal request, but the server is refusing to respond to it. For use when authentication is possible but has failed or not yet been provided
                402 to "Payment Required", // Reserved for future use
                403 to "Forbidden", // The request was a legal request, but the server is refusing to respond to it
                404 to "Not Found", // The requested page could not be found but may be available again in the future
                405 to "Method Not Allowed", // A request was made of a page using a request method not supported by that page
                406 to "Not Acceptable", // The server can only generate a response that is not accepted by the client
                407 to "Proxy Authentication Required", // The client must first authenticate itself with the proxy
                408 to "Request Timeout", // The server timed out waiting for the request
                409 to "Conflict", // The request could not be completed because of a conflict in the request
                410 to "Gone", // The requested page is no longer available
                411 to "Length Required", // The "Content-Length" is not defined. The server will not accept the request without it
                412 to "Precondition Failed", // The precondition given in the request evaluated to false by the server
                413 to "Request Entity Too Large", // The server will not accept the request, because the request entity is too large
                414 to "Request-URI Too Long", // The server will not accept the request, because the URL is too long. Occurs when you convert a POST request to a GET request with a long query information
                415 to "Unsupported Media Type", // The server will not accept the request, because the media type is not supported
                416 to "Requested Range Not Satisfiable", // The client has asked for a portion of the file, but the server cannot supply that portion
                417 to "Expectation Failed", // The server cannot meet the requirements of the Expect request-header field
                418 to "I'm a teapot", // This code was defined in 1998 as one of the traditional IETF April Fools' jokes
                421 to "Misdirected Request", // RFC 7540: The request was directed at a server that is not able to produce a response
                422 to "Unprocessable Entity", // WebDAV
                423 to "Locked", // WebDAV
                424 to "Failed Dependency", // WebDAV
                426 to "Upgrade Required", // The client should switch to a different protocol such as TLS/1.0, given in the Upgrade header field
                428 to "Precondition Required", // RFC 6585: The origin server requires the request to be conditional
                429 to "Too Many Requests", // RFC 6585: The user has sent too many requests in a given amount of time
                431 to "Request Header Fields Too Large", // RFC 6585: The server is unwilling to process the request because either an individual header field, or all the header fields collectively, are too large
                451 to "Unavailable For Legal Reasons", // RFC 7725: A server operator has received a legal demand to deny access to a resource or to a set of resources that includes the requested resource
                500 to "Internal Server Error", // A generic error message, given when no more specific message is suitable
                501 to "Not Implemented", // The server either does not recognize the request method, or it lacks the ability to fulfill the request
                502 to "Bad Gateway", // The server was acting as a gateway or proxy and received an invalid response from the upstream server
                503 to "Service Unavailable", // The server is currently unavailable (overloaded or down)
                504 to "Gateway Timeout", // The server was acting as a gateway or proxy and did not receive a timely response from the upstream server
                505 to "HTTP Version Not Supported", // The server does not support the HTTP protocol version used in the request
                506 to "Variant Also Negotiates", // RFC 2295: Transparent content negotiation for the request results in a circular reference
                507 to "Insufficient Storage", // WebDAV
                508 to "Loop Detected", // WebDAV
                510 to "Not Extended", // RFC 2774: Further extensions to the request are required for the server to fulfil it
                511 to "Network Authentication Required" // The client needs to authenticate to gain network access
        )
    }
}