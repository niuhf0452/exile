package com.github.niuhf0452.exile.examples

import com.github.niuhf0452.exile.inject.Inject
import com.github.niuhf0452.exile.inject.Named
import com.github.niuhf0452.exile.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

@Inject
@Singleton
@Named("file")
class FileUserRepository : UserRepository {
    private val users by lazy {
        val jsonString = javaClass.getResourceAsStream("/users.json").use { input ->
            input.readAllBytes().toString(Charsets.UTF_8)
        }
        val json = Json(JsonConfiguration.Stable)
        json.parse(UserList.serializer(), jsonString).users
    }

    override fun findUser(email: String): User? {
        return users.find { it.email == email }
    }

    @Serializable
    data class UserList(val users: List<User>)
}