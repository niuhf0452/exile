package com.github.niuhf0452.exile.examples

import com.github.niuhf0452.exile.inject.*

fun main() {
    @Inject
    class Test(val value: String?)

    val injector = Injector.builder()
            .addPackage(Test::class.java.packageName)
            .build()
    val obj = injector.getInstance(Test::class)
    println(obj.value)
}
