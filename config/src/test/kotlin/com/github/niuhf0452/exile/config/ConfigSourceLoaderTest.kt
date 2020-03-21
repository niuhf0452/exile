package com.github.niuhf0452.exile.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.ClasspathFileSource
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.standalone.JsonFileMappingsSource
import io.kotlintest.Spec
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
import java.net.URI
import java.net.URLEncoder
import java.util.*

class ConfigSourceLoaderTest : FunSpec() {
    private val server = WireMockServer(WireMockConfiguration.options().dynamicPort())
    private lateinit var vaultUrl: String

    init {
        test("A loader should load from system properties") {
            val p = UUID.randomUUID().toString()
            System.setProperty(p, "123")
            val config = Config.newBuilder().from(URI.create("sys://properties")).build()
            config.getString(p) shouldBe "123"
        }

        test("A loader should load from env") {
            val config = Config.newBuilder().from(URI.create("sys://env")).build()
            config.getString("HOME").shouldNotBeNull()
        }

        test("A loader should load from file") {
            val config = Config.newBuilder().from(javaClass.getResource("/test.conf").toURI()).build()
            config.getString("text") shouldBe "abc"
        }

        test("A loader should load from properties") {
            val uri = URI.create("properties://mem/?text=abc")
            val config = Config.newBuilder().from(uri).build()
            config.getString("text") shouldBe "abc"
        }

        test("A loader should load from simple config") {
            val content = """
                text = abc
                value = 123
            """.trimIndent()
            val uri = URI.create("simple://mem/?content=${URLEncoder.encode(content, Charsets.UTF_8)}")
            val config = Config.newBuilder().from(uri).build()
            config.getString("text") shouldBe "abc"
            config.getInt("value") shouldBe 123
        }

        test("A loader should load from vault") {
            val uri = URI.create("vault:/$vaultUrl?namespace=namespace&auth=TOKEN&token=token")
            val config = Config.newBuilder().from(uri).build()
            config.getInt("vault-value") shouldBe 123
        }
    }

    override fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        server.loadMappingsUsing(JsonFileMappingsSource(ClasspathFileSource("wiremock")))
        server.start()
        vaultUrl = server.baseUrl() + "/v1/kv/data/test"
    }

    override fun afterSpec(spec: Spec) {
        super.afterSpec(spec)
        server.stop()
    }
}