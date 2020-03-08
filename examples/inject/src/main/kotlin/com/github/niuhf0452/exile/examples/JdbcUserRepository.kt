package com.github.niuhf0452.exile.examples

import com.github.niuhf0452.exile.inject.Named

@Named("jdbc")
class JdbcUserRepository : UserRepository {
    override fun findUser(email: String): User? {
        return null
    }
}