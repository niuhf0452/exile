package com.github.niuhf0452.exile.netty

import com.github.niuhf0452.exile.inject.Inject
import com.github.niuhf0452.exile.inject.Named
import com.github.niuhf0452.exile.web.WebServer

@Inject
@Named("netty")
class WebServerFactory : WebServer.Factory by NettyServer.Factory()