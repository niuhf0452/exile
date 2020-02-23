package com.github.exile.inject

import com.github.exile.inject.impl.StaticBinder
import io.kotlintest.matchers.beInstanceOf
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.matchers.types.shouldNotBeSameInstanceAs
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FeatureSpec
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.reflect.full.starProjectedType

class InjectorTest : FeatureSpec({
    feature("static binder") {
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
                .addBinder(StaticBinder { c ->
                    c.bind(Int::class).toInstance(99)
                    c.bind(String::class).toInstance("bar", qualifiers = listOf(Qualifiers.named("foo")))
                    c.bind(String::class).toInstance("world", qualifiers = listOf(Qualifiers.named("hello")))
                    c.bind(Runnable::class).toProvider(
                            qualifiers = listOf(Qualifiers.named("foo"))) { TestRunnable() }
                    c.bind(Runnable::class).toProvider(
                            qualifiers = listOf(Qualifiers.named("bar")),
                            singleton = true) { TestRunnable() }
                    c.bind(Runnable::class.starProjectedType)
                            .toClass(TestRunnable::class, qualifiers = listOf(Qualifiers.named("hello")))
                    c.bind(Runnable::class).toType(
                            TestRunnable::class.starProjectedType,
                            qualifiers = listOf(Qualifiers.named("world")))
                    c.bind(object : TypeLiteral<Consumer<String>>() {}.type).toClass(TestConsumer::class)
                    c.bind(object : TypeLiteral<Supplier<TestRunnable>>() {}.type)
                            .toType(object : TypeLiteral<TestSupplier<TestRunnable>>() {}.type)
                })
                .build()

        scenario("bind to instance") {
            injector.getInstance(Int::class) shouldBe 99
        }

        scenario("bind to instance with qualifier") {
            injector.getInstance(String::class, listOf(Qualifiers.named("foo"))) shouldBe "bar"
            injector.getInstance(String::class, listOf(Qualifiers.named("hello"))) shouldBe "world"
        }

        scenario("bind to provider with qualifier") {
            injector.getInstance(Runnable::class,
                    listOf(Qualifiers.named("foo"))) should beInstanceOf<TestRunnable>()
        }

        scenario("bind to provider as singleton") {
            val a = injector.getInstance(Runnable::class, listOf(Qualifiers.named("foo")))
            val b = injector.getInstance(Runnable::class, listOf(Qualifiers.named("foo")))
            val c = injector.getInstance(Runnable::class, listOf(Qualifiers.named("bar")))
            val d = injector.getInstance(Runnable::class, listOf(Qualifiers.named("bar")))
            a shouldNotBeSameInstanceAs b
            c shouldBeSameInstanceAs d
        }

        scenario("bind to class and simple type") {
            injector.getInstance(Runnable::class, listOf(Qualifiers.named("hello")))
            injector.getInstance(Runnable::class, listOf(Qualifiers.named("world")))
        }

        scenario("bind from generic interface") {
            val type = object : TypeLiteral<Consumer<String>>() {}.type
            val t = injector.getBindings(TypeKey(type))
                    .getSingle()
                    .getInstance()
            t should beInstanceOf<TestConsumer>()
        }

        scenario("bind to generic class") {
            val type = object : TypeLiteral<Supplier<TestRunnable>>() {}.type
            val t = injector.getBindings(TypeKey(type))
                    .getSingle()
                    .getInstance()
            t should beInstanceOf<TestSupplier<*>>()
            (t as TestSupplier<*>).get() should beInstanceOf<TestRunnable>()
        }

        scenario("bind to mismatch type instance") {
            shouldThrow<IllegalArgumentException> {
                Injector.builder()
                        .addBinder(StaticBinder { c ->
                            c.bind(Int::class).toInstance("String")
                        })
                        .build()
            }
        }
        scenario("bind to mismatch type") {
            shouldThrow<IllegalArgumentException> {
                Injector.builder()
                        .addBinder(StaticBinder { c ->
                            c.bind(Int::class).toClass(String::class)
                        })
                        .build()
            }
        }
        scenario("bind to mismatch generic type") {
            shouldThrow<IllegalArgumentException> {
                Injector.builder()
                        .addBinder(StaticBinder { c ->
                            c.bind(object : TypeLiteral<Consumer<String>>() {}.type)
                                    .toClass(TestSupplier::class, listOf(TypeKey(String::class)))
                        })
                        .build()
            }
        }
        scenario("bind to mismatch type parameter") {
            shouldThrow<IllegalArgumentException> {
                Injector.builder()
                        .addBinder(StaticBinder { c ->
                            c.bind(object : TypeLiteral<Supplier<String>>() {}.type)
                                    .toType(object : TypeLiteral<TestSupplier<Int>>() {}.type)
                        })
                        .build()
            }
        }
    }

    feature("instantiate binder") {
        val injector = Injector.builder().build()

        scenario("create instance without parameters") {
            @Inject
            class TestClass

            injector.getInstance(TestClass::class)
        }

        scenario("create instance with parameters") {
            @Inject
            class ParameterClass

            @Inject
            class TestClass(val p: ParameterClass)

            val obj = injector.getInstance(TestClass::class)
            obj should beInstanceOf<TestClass>()
            obj.p should beInstanceOf<ParameterClass>()
        }

        scenario("inject companion object") {
            injector.getInstance(TestObject::class) shouldBeSameInstanceAs TestObject
        }

        scenario("create instance of generic class") {
            @Inject
            class TestClass<A>(val p: A)

            @Inject
            class ParameterClass

            val obj = injector.getBindings(object : TypeLiteral<TestClass<ParameterClass>>() {}.typeKey)
                    .getSingle()
                    .getInstance()
            obj should beInstanceOf<TestClass<*>>()
            (obj as TestClass<*>).p should { it is ParameterClass }
        }
    }

    feature("autowire binder") {
        val injector = Injector.builder().enableAutowire(listOf(this.javaClass.packageName)).build()
    }
})

object TestObject
