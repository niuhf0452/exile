package com.github.exile.inject

import com.github.exile.inject.impl.StaticBinder
import io.kotlintest.matchers.beInstanceOf
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.matchers.types.shouldNotBeSameInstanceAs
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FunSpec
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.reflect.full.starProjectedType

class StaticBinderTest : FunSpec({
    @Inject
    class TestRunnable : Runnable {
        override fun run() {
            println("TestClass run")
        }
    }

    @Inject
    class TestConsumer : Consumer<String> {
        override fun accept(t: String) {
            println(t)
        }
    }

    @Inject
    class TestSupplier<A>(val value: A) : Supplier<A> {
        override fun get(): A {
            return value
        }
    }

    val injector = Injector.builder()
            .enableStatic { c ->
                c.bind(Int::class).toInstance(99)
                c.bind(String::class).toInstance("bar", qualifiers = listOf(Qualifiers.named("foo")))
                c.bind(String::class).toInstance("world", qualifiers = listOf(Qualifiers.named("hello")))
                c.bind(Runnable::class).toProvider(
                        qualifiers = listOf(Qualifiers.named("foo"))) { TestRunnable() }
                c.bind(Runnable::class).toProvider(
                        qualifiers = listOf(Qualifiers.named("bar"), Qualifiers.singleton())) { TestRunnable() }
                c.bind(Runnable::class.starProjectedType).toType(
                        TypeKey(TestRunnable::class),
                        qualifiers = listOf(Qualifiers.named("hello")))
                c.bind(Runnable::class).toType(
                        TypeKey(TestRunnable::class),
                        qualifiers = listOf(Qualifiers.named("world")))
                c.bind(object : TypeLiteral<Consumer<String>>() {}.type)
                        .toType(TypeKey(TestConsumer::class))
                c.bind(object : TypeLiteral<Supplier<TestRunnable>>() {}.type)
                        .toType(object : TypeLiteral<TestSupplier<TestRunnable>>() {}.typeKey)
            }
            .build()

    test("A StaticBinder should bind to instance") {
        injector.getInstance(Int::class) shouldBe 99
    }

    test("A StaticBinder should bind to instance with qualifier") {
        injector.getInstance(String::class, listOf(Qualifiers.named("foo"))) shouldBe "bar"
        injector.getInstance(String::class, listOf(Qualifiers.named("hello"))) shouldBe "world"
    }

    test("A StaticBinder should bind to provider with qualifier") {
        injector.getInstance(Runnable::class,
                listOf(Qualifiers.named("foo"))) should beInstanceOf<TestRunnable>()
    }

    test("A StaticBinder should bind to provider as singleton") {
        val a = injector.getInstance(Runnable::class, listOf(Qualifiers.named("foo")))
        val b = injector.getInstance(Runnable::class, listOf(Qualifiers.named("foo")))
        val c = injector.getInstance(Runnable::class, listOf(Qualifiers.named("bar")))
        val d = injector.getInstance(Runnable::class, listOf(Qualifiers.named("bar")))
        a shouldNotBeSameInstanceAs b
        c shouldBeSameInstanceAs d
    }

    test("A StaticBinder should bind to class and simple type") {
        injector.getInstance(Runnable::class, listOf(Qualifiers.named("hello")))
        injector.getInstance(Runnable::class, listOf(Qualifiers.named("world")))
    }

    test("A StaticBinder should bind from generic interface") {
        val type = object : TypeLiteral<Consumer<String>>() {}.type
        val t = injector.getBindings(TypeKey(type))
                .getSingle()
                .getInstance()
        t should beInstanceOf<TestConsumer>()
    }

    test("A StaticBinder should bind to generic class") {
        val type = object : TypeLiteral<Supplier<TestRunnable>>() {}.type
        val t = injector.getBindings(TypeKey(type))
                .getSingle()
                .getInstance()
        t should beInstanceOf<TestSupplier<*>>()
        (t as TestSupplier<*>).get() should beInstanceOf<TestRunnable>()
    }

    test("A StaticBinder should NOT bind to mismatch type instance") {
        shouldThrow<IllegalArgumentException> {
            Injector.builder()
                    .addBinder(StaticBinder { c ->
                        c.bind(Int::class).toInstance("String")
                    })
                    .build()
        }
    }
    test("A StaticBinder should NOT bind to mismatch type") {
        shouldThrow<IllegalArgumentException> {
            Injector.builder()
                    .addBinder(StaticBinder { c ->
                        c.bind(Int::class).toType(TypeKey(String::class))
                    })
                    .build()
        }
    }
    test("A StaticBinder should NOT bind to mismatch generic type") {
        shouldThrow<IllegalArgumentException> {
            Injector.builder()
                    .addBinder(StaticBinder { c ->
                        c.bind(object : TypeLiteral<Consumer<String>>() {}.type)
                                .toType(object : TypeLiteral<TestSupplier<String>>() {}.typeKey)
                    })
                    .build()
        }
    }
    test("A StaticBinder should NOT bind to mismatch type parameter") {
        shouldThrow<IllegalArgumentException> {
            Injector.builder()
                    .addBinder(StaticBinder { c ->
                        c.bind(object : TypeLiteral<Supplier<String>>() {}.type)
                                .toType(object : TypeLiteral<TestSupplier<Int>>() {}.typeKey)
                    })
                    .build()
        }
    }
})
