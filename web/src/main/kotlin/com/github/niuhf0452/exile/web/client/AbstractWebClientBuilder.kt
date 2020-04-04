package com.github.niuhf0452.exile.web.client

import com.github.niuhf0452.exile.web.WebClient
import com.github.niuhf0452.exile.web.WebInterceptor
import com.github.niuhf0452.exile.web.internal.InterceptorList
import java.time.Duration
import javax.net.ssl.SSLContext

abstract class AbstractWebClientBuilder : WebClient.Builder {
    private var maxKeepAliveConnectionSize = 10
    private var connectTimeout = Duration.ofSeconds(5)
    private var requestTimeout = Duration.ofSeconds(15)
    private var sslContext: SSLContext? = null
    private val interceptors = mutableListOf<WebInterceptor>()

    protected abstract fun createClient(maxKeepAliveConnectionSize: Int,
                                        connectTimeout: Duration,
                                        requestTimeout: Duration,
                                        sslContext: SSLContext?,
                                        interceptors: InterceptorList): WebClient

    override fun maxKeepAliveConnectionSize(value: Int): WebClient.Builder {
        maxKeepAliveConnectionSize = value
        return this
    }

    override fun connectTimeout(value: Duration): WebClient.Builder {
        connectTimeout = value
        return this
    }

    override fun requestTimeout(value: Duration): WebClient.Builder {
        requestTimeout = value
        return this
    }

    override fun sslContext(value: SSLContext): WebClient.Builder {
        sslContext = value
        return this
    }

    override fun addInterceptor(interceptor: WebInterceptor): WebClient.Builder {
        interceptors.add(interceptor)
        return this
    }

    override fun build(): WebClient {
        return createClient(maxKeepAliveConnectionSize, connectTimeout, requestTimeout, sslContext,
                InterceptorList(interceptors))
    }
}