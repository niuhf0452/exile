package com.github.niuhf0452.exile.web.internal

import com.github.niuhf0452.exile.web.WebResponse

class DirectResponseException(val response: WebResponse<ByteArray>)
    : RuntimeException()