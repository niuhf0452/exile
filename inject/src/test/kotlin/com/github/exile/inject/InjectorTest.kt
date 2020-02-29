package com.github.exile.inject

import com.github.exile.inject.autowire.AutowireTestInterfaceA
import com.github.exile.inject.autowire.TestClassA
import com.github.exile.inject.impl.ClassgraphScanner
import io.kotlintest.matchers.collections.shouldExist
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
import kotlinx.coroutines.delay

class InjectorTest : FunSpec({
    test("An Injector should close the binders and filters correctly") {
        var binderClosed = false
        var filterClosed = false
        Injector.builder()
                .addBinder(object : Injector.Binder, AutoCloseable {
                    override fun bind(key: TypeKey, context: Injector.BindingContext) = Unit

                    override fun close() {
                        binderClosed = true
                    }
                })
                .addFilter(object : Injector.Filter, AutoCloseable {
                    override val order: Int
                        get() = 0

                    override fun filter(binding: Injector.Binding): Injector.Binding {
                        return binding
                    }

                    override fun close() {
                        filterClosed = true
                    }
                })
                .build()
                .close()
        binderClosed shouldBe true
        filterClosed shouldBe true
    }

    test("An Injector should work in EAGER mode") {
        val injector = Injector.builder()
                .loadingMode(Injector.LoadingMode.EAGER)
                .scanner(ClassgraphScanner(listOf(TestClassA::class.java.packageName)))
                .enableAutowire()
                .enableScope()
                .build()
        val key = TypeKey(AutowireTestInterfaceA::class)
        injector.preparedBindings() shouldExist { it.key == key }
    }

    test("An Injector should work in ASYNC mode") {
        val injector = Injector.builder()
                .loadingMode(Injector.LoadingMode.ASYNC)
                .scanner(ClassgraphScanner(listOf(TestClassA::class.java.packageName)))
                .enableAutowire()
                .enableScope()
                .build()
        delay(100)
        val key = TypeKey(AutowireTestInterfaceA::class)
        injector.preparedBindings() shouldExist { it.key == key }
    }
})