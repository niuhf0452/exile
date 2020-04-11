package com.github.niuhf0452.exile.netty

import com.github.niuhf0452.exile.web.ClientAndServerTest
import com.github.niuhf0452.exile.web.client.JdkHttpClient

class NettyServerTest : ClientAndServerTest(JdkHttpClient.Builder(), WebServerFactory())
