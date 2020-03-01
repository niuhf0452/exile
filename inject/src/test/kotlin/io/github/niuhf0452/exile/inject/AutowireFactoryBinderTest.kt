package io.github.niuhf0452.exile.inject

import io.github.niuhf0452.exile.inject.autowire.AutowireTestInterfaceA
import io.github.niuhf0452.exile.inject.autowire.AutowireTestInterfaceK
import io.github.niuhf0452.exile.inject.autowire.TestClassKFactory
import io.github.niuhf0452.exile.inject.impl.AutowireFactoryBinder
import io.github.niuhf0452.exile.inject.impl.ClassgraphScanner
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.specs.FunSpec

class AutowireFactoryBinderTest : FunSpec({
    val injector = Injector.builder()
            .scanner(ClassgraphScanner(listOf(AutowireTestInterfaceA::class.java.packageName)))
            .addBinder(AutowireFactoryBinder())
            .enableScope()
            .build()

    test("An AutowireFactoryBinder should work") {
        injector.getInstance(AutowireTestInterfaceK::class)
    }

    test("An AutowireFactoryBinder should bind to method") {
        val binding = injector.getBindings(TypeKey(AutowireTestInterfaceK::class))
                .getSingle()
        binding.toString() shouldContain "Method(${TestClassKFactory::get})"
    }
})
