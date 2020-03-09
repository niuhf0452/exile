package com.github.niuhf0452.exile.examples

import com.github.niuhf0452.exile.inject.*

fun main() {
    @Inject
    class Test

    @Inject
    class Test2(
            @Named("in-memory")
            val provider: Provider<UserRepository>
    )

    val injector = Injector.builder()
            .addPackage(Test::class.java.packageName)
            .enableAutowire()
            .build()
    val provider = injector.getProvider(Test::class)
    println(provider.get())

    val test2 = injector.getInstance(Test2::class)
    println(test2.provider.get())
}
