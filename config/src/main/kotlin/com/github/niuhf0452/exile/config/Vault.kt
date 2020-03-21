package com.github.niuhf0452.exile.config

import com.github.niuhf0452.exile.config.source.VaultSource

data class VaultConfig(
        val url: String,
        val namespace: String = "",
        val timeoutInSeconds: Long = 5000L,
        val auth: AuthMethod = AuthMethod.TOKEN,
        val token: String = "",
        val kubernetes: Kubernetes = Kubernetes(""),
        val approle: AppRole = AppRole("", "")
) {
    enum class AuthMethod {
        TOKEN, KUBERNETES, APPROLE
    }

    data class Kubernetes(
            val role: String,
            val serviceTokenFile: String = "/var/run/secrets/kubernetes.io/serviceaccount/token",
            val url: String = "/v1/auth/kubernetes/login"
    )

    data class AppRole(
            val roleId: String,
            val secretId: String,
            val url: String = "/v1/auth/approle/login"
    )
}

fun Config.Builder.fromVault(config: VaultConfig): Config.Builder {
    return from(VaultSource(config))
}