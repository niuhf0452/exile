package com.github.niuhf0452.exile.examples

import com.github.niuhf0452.exile.inject.*

fun main() {
    @Factory
    class SpringStyleConfiguration {
        @Factory
        @Named("factory-injected")
        fun createUserRepository(): UserRepository {
            return FileUserRepository()
        }
    }

    val injector = Injector.builder()
            .addPackage(SpringStyleConfiguration::class.java.packageName)
            .enableAutowire()
            .build()
    val userRepository = injector.getInstance(UserRepository::class,
            qualifiers = listOf(Qualifiers.named("factory-injected")))
    userRepository.findUser("chao.ma@example.com")
}