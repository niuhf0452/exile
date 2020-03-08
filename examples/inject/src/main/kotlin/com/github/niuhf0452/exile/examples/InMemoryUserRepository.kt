package com.github.niuhf0452.exile.examples

import com.github.niuhf0452.exile.inject.Inject
import com.github.niuhf0452.exile.inject.Named

@Inject
@Named("in-memory")
class InMemoryUserRepository : UserRepository {
    private val users = listOf<User>(
            User(101L, "kristin.graham@example.com", "Kristin Graham"),
            User(102L, "tanya.deckow@example.com", "Tanya Deckow")
    )

    override fun findUser(email: String): User? {
        return users.find { it.email == email }
    }
}
