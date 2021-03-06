package com.github.niuhf0452.exile.inject

import com.github.niuhf0452.exile.inject.binder.AutowireFactoryBinder
import com.github.niuhf0452.exile.test.factory0.AutowireTestInterfaceA
import com.github.niuhf0452.exile.test.factory0.TestClassAFactory
import com.github.niuhf0452.exile.test.factory1.TestClass
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FunSpec

class AutowireFactoryBinderTest : FunSpec({
    fun forCorrectCase(): Injector {
        return Injector.builder()
                .addPackage(AutowireTestInterfaceA::class.java.packageName)
                .addBinder(AutowireFactoryBinder())
                .enableScope()
                .build()
    }

    test("An AutowireFactoryBinder should work") {
        val injector = forCorrectCase()
        injector.getInstance(AutowireTestInterfaceA::class)
    }

    test("An AutowireFactoryBinder should bind to method") {
        val injector = forCorrectCase()
        val binding = injector.getBindings(TypeKey(AutowireTestInterfaceA::class))
                .getSingle()
        binding.toString() shouldContain "Method(${TestClassAFactory::get})"
    }

    test("An AutowireFactoryBinder should NOT bind to generic method") {
        val injector = Injector.builder()
                .addPackage(TestClass::class.java.packageName)
                .addBinder(AutowireFactoryBinder())
                .build()
        shouldThrow<InjectException> {
            injector.getInstance(String::class)
        }
    }
})
