package com.github.niuhf0452.exile.inject

import com.github.niuhf0452.exile.inject.binder.AutowireBinder
import com.github.niuhf0452.exile.test.autowire.*
import io.kotlintest.matchers.beInstanceOf
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.matchers.types.shouldNotBeSameInstanceAs
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FunSpec
import com.github.niuhf0452.exile.test.autowire.TestClassA as TestClassA1

class AutowireBinderTest : FunSpec({
    val injector = Injector.builder()
            .addPackage(AutowireTestInterfaceA::class.java.packageName)
            .addBinder(AutowireBinder())
            .enableScope()
            .build()

    test("An AutowireBinder should inject instance of interface") {
        injector.getInstance(AutowireTestInterfaceA::class) should beInstanceOf<TestClassA1>()
    }

    test("An AutowireBinder should bind classes to interface with qualifiers") {
        injector.getInstance(AutowireTestInterfaceB::class,
                listOf(Qualifiers.named("foo"))) should beInstanceOf<TestClassB1>()
        injector.getInstance(AutowireTestInterfaceB::class,
                listOf(Qualifiers.named("bar"))) should beInstanceOf<TestClassB2>()
    }

    test("An AutowireBinder should bind object to interface") {
        injector.getInstance(AutowireTestInterfaceC::class) shouldBe TestObjectC
    }

    test("An AutowireBinder should bind classes to abstract class") {
        injector.getInstance(AutowireTestInterfaceD::class) should beInstanceOf<TestClassD>()
    }

    test("An AutowireBinder should bind class to generic interface") {
        injector.getBindings(object : TypeLiteral<AutowireTestInterfaceE<String>>() {}.typeKey)
                .getSingle().getInstance() should beInstanceOf<TestClassE1>()
        injector.getBindings(object : TypeLiteral<AutowireTestInterfaceE<Int>>() {}.typeKey)
                .getSingle().getInstance() should beInstanceOf<TestClassE2>()
    }

    test("An AutowireBinder should bind to same instance for @Singleton") {
        val a = injector.getInstance(TestClassF::class)
        val b = injector.getInstance(TestClassF::class)
        a shouldBeSameInstanceAs b
    }

    test("An AutowireBinder should bind to difference instances without @Singleton") {
        val a = injector.getInstance(TestClassA1::class)
        val b = injector.getInstance(TestClassA1::class)
        a shouldNotBeSameInstanceAs b
    }

    test("An AutowireBinder should NOT bind abstract class") {
        shouldThrow<InjectException> {
            injector.getInstance(AutowireTestInterfaceG::class)
        }
    }

    test("An AutowireBinder should NOT bind generic class") {
        shouldThrow<InjectException> {
            injector.getBindings(object : TypeLiteral<AutowireTestInterfaceH<Int>>() {}.typeKey)
                    .getSingle()
        }
    }

    test("An AutowireBinder should NOT bind class without @Inject") {
        shouldThrow<InjectException> {
            injector.getInstance(AutowireTestInterfaceI::class)
        }
    }

    test("An AutowireBinder should NOT bind class to interface with @Excludes") {
        shouldThrow<InjectException> {
            injector.getInstance(AutowireTestInterfaceJ::class)
        }
    }
})
