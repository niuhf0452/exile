package com.github.niuhf0452.exile.examples

import com.github.niuhf0452.exile.inject.Injector
import com.github.niuhf0452.exile.inject.Qualifiers
import com.github.niuhf0452.exile.inject.TypeKey
import com.github.niuhf0452.exile.inject.getInstance

fun main() {
    val injector = Injector.builder()
            .addPackage("com.github.niuhf0452.exile.examples")
            .enableAutowire()
            .enableScope()
            .build()
    val fileRepository = injector.getInstance(UserRepository::class, listOf(Qualifiers.named("file")))
    val memRepository = injector.getInstance(UserRepository::class, listOf(Qualifiers.named("in-memory")))
    val john = fileRepository.findUser("john.smith@example.com")
    val susan = memRepository.findUser("susan.zhang@example.com")
    println(john)
    println(susan)
    val bindings = injector.getBindings(TypeKey(UserRepository::class))
    bindings.forEach { binding ->
        val repository = binding.getInstance() as UserRepository
        val user = repository.findUser("tanya.deckow@example.com")
        println(user)
    }
}