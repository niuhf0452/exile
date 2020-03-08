package com.github.niuhf0452.exile.examples

import com.github.niuhf0452.exile.inject.Injector
import com.github.niuhf0452.exile.inject.Qualifiers
import com.github.niuhf0452.exile.inject.TypeKey
import com.github.niuhf0452.exile.inject.getInstance

fun main() {
    val injector = Injector.builder()
            .addPackage("com.github.niuhf0452.exile.examples")
            .enableStatic { c ->
                c.bind(UserRepository::class).toInstance(FileUserRepository(),
                        qualifiers = listOf(Qualifiers.named("file")))
                c.bind(UserRepository::class).toType(TypeKey(InMemoryUserRepository::class),
                        qualifiers = listOf(Qualifiers.named("in-memory")))
            }
            .build()
    val fileRepository = injector.getInstance(UserRepository::class, listOf(Qualifiers.named("file")))
    val memRepository = injector.getInstance(UserRepository::class, listOf(Qualifiers.named("in-memory")))
    val john = fileRepository.findUser("john.smith@example.com")
    val susan = memRepository.findUser("susan.zhang@example.com")
    println(john)
    println(susan)
}