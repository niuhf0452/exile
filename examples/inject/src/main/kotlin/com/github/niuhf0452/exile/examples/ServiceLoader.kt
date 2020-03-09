package com.github.niuhf0452.exile.examples

import com.github.niuhf0452.exile.inject.Injector
import com.github.niuhf0452.exile.inject.Qualifiers
import com.github.niuhf0452.exile.inject.TypeKey
import com.github.niuhf0452.exile.inject.getInstance

fun main() {
    val injector = Injector.builder()
            .addPackage("com.github.niuhf0452.exile.examples")
            .enableServiceLoader()
            .build()
    val userRepository = injector.getInstance(UserRepository::class,
            qualifiers = listOf(Qualifiers.named("jdbc")))
    val john = userRepository.findUser("john.smith@example.com")
    println(john)
}