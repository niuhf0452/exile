package com.github.niuhf0452.exile.web.impl

import com.github.niuhf0452.exile.web.WebResponse

class DirectResponseException(val response: WebResponse<ByteArray>)
    : RuntimeException()