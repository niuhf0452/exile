package com.github.niuhf0452.exile.examples

import com.github.niuhf0452.exile.inject.*
import com.github.niuhf0452.exile.inject.impl.ByteBuddyEnhancer
import kotlin.reflect.KFunction

fun main() {
    class LoggingInterceptor : ClassInterceptor {
        override fun intercept(method: KFunction<*>): MethodInterceptor<*, *>? {
            if (method.name != "findUser") {
                return null
            }
            return object : MethodInterceptor<Any, Unit> {
                override fun beforeCall(instance: Any, args: List<Any?>) {
                    println("before call")
                }

                override fun afterCall(instance: Any, args: List<Any?>, exception: Exception?, returnValue: Any?, state: Unit) {
                    println("after call")
                }
            }
        }
    }

    val injector = Injector.builder()
            .addPackage(UserRepository::class.java.packageName)
            .enableAutowire()
            .addInterceptor(LoggingInterceptor())
            .enhancer(ByteBuddyEnhancer())
            .build()
    val userRepository = injector.getInstance(UserRepository::class,
            qualifiers = listOf(Qualifiers.named("file")))
    userRepository.findUser("chris@example.com")
}