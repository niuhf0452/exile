package com.github.niuhf0452.exile.examples

import kotlinx.serialization.Serializable

interface UserRepository {
    fun findUser(email: String): User?
}

@Serializable
data class User(val id: Long, val email: String, val name: String)