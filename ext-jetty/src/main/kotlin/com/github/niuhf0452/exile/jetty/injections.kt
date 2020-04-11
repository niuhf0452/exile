package com.github.niuhf0452.exile.jetty

import com.github.niuhf0452.exile.inject.Inject
import com.github.niuhf0452.exile.inject.Named
import com.github.niuhf0452.exile.web.WebServer

@Inject
@Named("jetty")
class WebServerFactory : WebServer.Factory by JettyServer.Factory()