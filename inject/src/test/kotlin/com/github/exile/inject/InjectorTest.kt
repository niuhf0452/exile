package com.github.exile.inject

import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec

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
})