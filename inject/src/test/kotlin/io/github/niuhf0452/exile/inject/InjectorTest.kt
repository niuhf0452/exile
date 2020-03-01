package io.github.niuhf0452.exile.inject

import io.github.niuhf0452.exile.inject.impl.ClassgraphScanner
import io.kotlintest.matchers.beInstanceOf
import io.kotlintest.should
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

    test("An Injector should inject Provider of class") {
        @Inject
        class TestClass

        val injector = Injector.builder()
                .scanner(ClassgraphScanner(listOf(InjectorTest::class.java.packageName)))
                .enableAutowire()
                .build()
        injector.getBindings(object : TypeLiteral<Provider<TestClass>>() {}.typeKey)
                .getSingle()
                .getInstance()
    }

    test("An Injector should inject Provider of interface") {
        @Inject
        class TestClass : TestInterface

        val injector = Injector.builder()
                .scanner(ClassgraphScanner(listOf(InjectorTest::class.java.packageName)))
                .enableAutowire()
                .build()
        val provider = injector.getBindings(object : TypeLiteral<Provider<TestInterface>>() {}.typeKey)
                .getSingle()
                .getInstance() as Provider<*>
        provider.get() should beInstanceOf<TestClass>()
    }
}) {
    interface TestInterface
}