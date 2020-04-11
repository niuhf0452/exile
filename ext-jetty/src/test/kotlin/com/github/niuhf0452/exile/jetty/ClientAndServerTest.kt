package com.github.niuhf0452.exile.jetty

import com.github.niuhf0452.exile.web.ClientAndServerTest
import com.github.niuhf0452.exile.web.client.JdkHttpClient

class JettyServerTest : ClientAndServerTest(JdkHttpClient.Builder(), WebServerFactory())
