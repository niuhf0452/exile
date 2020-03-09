package com.github.niuhf0452.exile.examples

import com.github.niuhf0452.exile.inject.Inject
import com.github.niuhf0452.exile.inject.Injector
import com.github.niuhf0452.exile.inject.Singleton
import com.github.niuhf0452.exile.inject.getInstance

fun main() {
    abstract class Car

    @Inject
    @Singleton
    class TheUniqueCar : Car()

    val injector = Injector.builder()
            .addPackage(Car::class.java.packageName)
            .enableAutowire()
            .enableScope()
            .build()
    val a = injector.getInstance(Car::class)
    val b = injector.getInstance(Car::class)
    println(a == b)
}