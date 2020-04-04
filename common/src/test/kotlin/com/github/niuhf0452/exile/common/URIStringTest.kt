package com.github.niuhf0452.exile.common

import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
import kotlinx.serialization.json.Json
import java.net.URI

class URIStringTest : FunSpec({
    test("A URIString should return URI") {
        val uri = "http://localhost:123/abc"
        URIString.parse(uri).toURI() shouldBe URI.create(uri)
    }

    test("URIString should implement equals, hashCode and toString") {
        val uri = "http://localhost:123/abc"
        URIString.parse(uri) shouldBe URIString.parse(uri)
        URIString.parse(uri).hashCode() shouldBe URIString.parse(uri).hashCode()
        URIString.parse(uri).toString() shouldBe URIString.parse(uri).toString()
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    test("A URIString should be serialized") {
        val obj = URIString.parse("http://localhost:123/abc")
        val json = Json.stringify(URIString.serializer(), obj)
        val obj2 = Json.parse(URIString.serializer(), json)
        obj2 shouldBe obj
    }
})