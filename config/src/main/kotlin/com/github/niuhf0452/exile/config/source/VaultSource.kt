package com.github.niuhf0452.exile.config.source

import com.github.niuhf0452.exile.config.Config
import com.github.niuhf0452.exile.config.ConfigException
import com.github.niuhf0452.exile.config.ConfigValue
import com.github.niuhf0452.exile.config.VaultConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration

@OptIn(UnstableDefault::class)
class VaultSource(
        private val config: VaultConfig
) : Config.Source {
    override fun load(): Iterable<ConfigValue> {
        val token = login()
        val values = loadValues(token)
        return values.map { (k, v) -> ConfigValue(this, k, v) }
    }

    private fun login(): String {
        return when (config.auth) {
            VaultConfig.AuthMethod.TOKEN -> config.token
            VaultConfig.AuthMethod.KUBERNETES -> loginByKubernetes()
            VaultConfig.AuthMethod.APPROLE -> loginByAppRole()
        }
    }

    private fun loginByKubernetes(): String {
        val jwt = Files.readString(Paths.get(config.kubernetes.serviceTokenFile))
        val authRequest = KubernetesAuthRequest(config.kubernetes.role, jwt)
        val requestJson = json.stringify(KubernetesAuthRequest.serializer(), authRequest)
        val responseJson = request(getLoginUrl(config.kubernetes.url), requestJson = requestJson)
        val authResponse = json.parse(AuthResponse.serializer(), responseJson)
        return authResponse.auth.clientToken
    }

    private fun loginByAppRole(): String {
        val authRequest = AppRoleRequest(config.approle.roleId, config.approle.secretId)
        val requestJson = json.stringify(AppRoleRequest.serializer(), authRequest)
        val responseJson = request(getLoginUrl(config.approle.url), requestJson = requestJson)
        val authResponse = json.parse(AuthResponse.serializer(), responseJson)
        return authResponse.auth.clientToken
    }

    private fun request(uri: URI, token: String? = null, requestJson: String? = null): String {
        val builder = HttpRequest.newBuilder().uri(uri)
                .timeout(Duration.ofSeconds(config.timeoutInSeconds))
        if (config.namespace.isNotBlank()) {
            builder.header(VAULT_NAMESPACE_HEADER, config.namespace)
        }
        if (token != null) {
            builder.header(VAULT_TOKEN_HEADER, token)
        }
        val request = if (requestJson == null) {
            builder.GET().build()
        } else {
            builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build()
        }
        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() != 200) {
            val message = try {
                response.body().readAllBytes().toString(Charsets.UTF_8)
            } catch (ex: Exception) {
                "(fail to read body)"
            }
            throw ConfigException("Fail to login into Vault: $uri - ${response.statusCode()} - $message")
        }
        return response.body().readAllBytes().toString(Charsets.UTF_8)
    }

    private fun getLoginUrl(url: String): URI {
        if (url.startsWith("http:") || url.startsWith("https:")) {
            return URI.create(url)
        }
        val base = URI.create(config.url)
        return base.resolve("/").resolve(url)
    }

    private fun loadValues(token: String): Map<String, String> {
        val responseJson = request(URI.create(config.url), token = token)
        val response = json.parse(KvResponse.serializer(), responseJson)
        return response.data.data
    }

    companion object {
        private val client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build()

        private const val VAULT_NAMESPACE_HEADER = "X-Vault-Namespace"
        private const val VAULT_TOKEN_HEADER = "X-Vault-Token"
        private val json = Json(JsonConfiguration.Stable.copy(ignoreUnknownKeys = true))
    }

    @Serializable
    private data class KubernetesAuthRequest(val role: String, val jwt: String)

    @Serializable
    private data class AppRoleRequest(
            @SerialName("role_id")
            val roleId: String,
            @SerialName("secret_id")
            val secretId: String
    )

    @Serializable
    private data class AuthResponse(val auth: Auth) {
        @Serializable
        data class Auth(
                @SerialName("client_token")
                val clientToken: String
        )
    }

    @Serializable
    private data class KvResponse(val data: Data) {
        @Serializable
        data class Data(val data: Map<String, String>)
    }
}
