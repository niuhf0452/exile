package com.github.niuhf0452.exile.config

import com.github.niuhf0452.exile.config.VaultConfig.AuthMethod
import com.github.niuhf0452.exile.config.source.VaultSource
import kotlinx.serialization.Serializable

/**
 * A configuration of Vault.
 */
@Serializable
data class VaultConfig(
        /**
         * The url to read configuration from.
         */
        val url: String,
        /**
         * The namespace of Vault. Empty string means no namespace.
         */
        val namespace: String = "",
        /**
         * The timeout for calling Vault API.
         */
        val timeoutInSeconds: Long = 5000L,
        /**
         * The authentication method.
         */
        val auth: AuthMethod = AuthMethod.TOKEN,
        /**
         * The token used for authentication, only take effect if [auth] is [AuthMethod.TOKEN].
         */
        val token: String = "",
        /**
         * The configuration used for kubernetes authentication, only take effect if [auth] is [AuthMethod.KUBERNETES].
         */
        val kubernetes: Kubernetes = Kubernetes(""),
        /**
         * The configuration used for approle authentication, only take effect if [auth] is [AuthMethod.APPROLE].
         */
        val approle: AppRole = AppRole("", "")
) {
    enum class AuthMethod {
        TOKEN, KUBERNETES, APPROLE
    }

    @Serializable
    data class Kubernetes(
            val role: String,
            val serviceTokenFile: String = "/var/run/secrets/kubernetes.io/serviceaccount/token",
            val url: String = "/v1/auth/kubernetes/login"
    )

    @Serializable
    data class AppRole(
            val roleId: String,
            val secretId: String,
            val url: String = "/v1/auth/approle/login"
    )
}

/**
 * Add a source which loads values from Vault.
 */
fun Config.Builder.fromVault(config: VaultConfig): Config.Builder {
    return from(VaultSource(config))
}