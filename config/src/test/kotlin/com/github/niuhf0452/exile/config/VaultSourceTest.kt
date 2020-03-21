package com.github.niuhf0452.exile.config

import com.github.niuhf0452.exile.config.source.SimpleConfigSource
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.ClasspathFileSource
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.standalone.JsonFileMappingsSource
import io.kotlintest.Spec
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
import java.nio.file.Files

class VaultSourceTest : FunSpec() {
    private val server = WireMockServer(WireMockConfiguration.options().dynamicPort())

    init {
        val vaultUrl = server.baseUrl() + "/v1/kv/data/test"

        test("A VaultSource should read from vault") {
            val config = Config.newBuilder()
                    .fromVault(VaultConfig(
                            url = vaultUrl,
                            namespace = "namespace",
                            token = "token",
                            auth = VaultConfig.AuthMethod.TOKEN
                    ))
                    .build()
            config.getInt("vault-value") shouldBe 123
        }

        @Suppress("BlockingMethodInNonBlockingContext")
        test("A VaultSource should login by kubernetes") {
            val path = Files.createTempFile("token", "")
            try {
                Files.writeString(path, "jwt_token")
                val config = Config.newBuilder()
                        .fromVault(VaultConfig(
                                url = vaultUrl,
                                namespace = "namespace",
                                auth = VaultConfig.AuthMethod.KUBERNETES,
                                kubernetes = VaultConfig.Kubernetes(
                                        role = "role",
                                        serviceTokenFile = path.toString()
                                )
                        ))
                        .build()
                config.getInt("vault-value") shouldBe 123
            } finally {
                Files.delete(path)
            }
        }

        test("A VaultSource should login by app role") {
            val config = Config.newBuilder()
                    .fromVault(VaultConfig(
                            url = vaultUrl,
                            namespace = "namespace",
                            auth = VaultConfig.AuthMethod.APPROLE,
                            approle = VaultConfig.AppRole("role-id", "secret-id")
                    ))
                    .build()
            config.getInt("vault-value") shouldBe 123
        }

        test("A VaultSource should be used in auto configure") {
            val config = Config.newBuilder()
                    .autoConfigure("/application-test-4.*", overwrite = SimpleConfigSource("""
                        config.sources.vault.url = $vaultUrl
                    """.trimIndent()))
                    .build()
            config.getInt("vault-value") shouldBe 123
        }
    }

    override fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        server.loadMappingsUsing(JsonFileMappingsSource(ClasspathFileSource("wiremock")))
        server.start()
    }

    override fun afterSpec(spec: Spec) {
        super.afterSpec(spec)
        server.stop()
    }
}