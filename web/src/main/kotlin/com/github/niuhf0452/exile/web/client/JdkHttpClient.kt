package com.github.niuhf0452.exile.web.client

import com.github.niuhf0452.exile.web.MultiValueMap
import com.github.niuhf0452.exile.web.WebClient
import com.github.niuhf0452.exile.web.WebRequest
import com.github.niuhf0452.exile.web.WebResponse
import com.github.niuhf0452.exile.web.internal.InterceptorList
import kotlinx.coroutines.future.await
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import javax.net.ssl.SSLContext

class JdkHttpClient(
        private val client: HttpClient,
        private val requestTimeout: Duration,
        override val interceptors: InterceptorList
) : AbstractWebClient() {
    override suspend fun backendSend(request: WebRequest<ByteArray>): WebResponse<ByteArray> {
        val builder = HttpRequest.newBuilder()
                .uri(request.uri)
                .timeout(requestTimeout)
        request.headers.forEach { name ->
            request.headers.get(name).forEach { value ->
                builder.header(name, value)
            }
        }
        val body = if (request.entity == null) {
            HttpRequest.BodyPublishers.noBody()
        } else {
            HttpRequest.BodyPublishers.ofByteArray(request.entity)
        }
        val req = builder.method(request.method, body)
                .build()
        val resp = client.sendAsync(req, HttpResponse.BodyHandlers.ofByteArray()).await()
        val headers = MultiValueMap(false)
        resp.headers().map().forEach { (k, v) ->
            headers.set(k, v)
        }
        var entity = resp.body()
        if (entity != null && entity.isEmpty()) {
            entity = null
        }
        return WebResponse(resp.statusCode(), headers, entity)
    }

    class Builder : AbstractWebClientBuilder() {
        override fun createClient(maxKeepAliveConnectionSize: Int,
                                  connectTimeout: Duration,
                                  requestTimeout: Duration,
                                  sslContext: SSLContext?,
                                  interceptors: InterceptorList): WebClient {
            val clientBuilder = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .connectTimeout(connectTimeout)
                    .followRedirects(HttpClient.Redirect.NEVER)
            if (sslContext != null) {
                clientBuilder.sslContext(sslContext)
            }
            return JdkHttpClient(clientBuilder.build(), requestTimeout, interceptors)
        }
    }
}