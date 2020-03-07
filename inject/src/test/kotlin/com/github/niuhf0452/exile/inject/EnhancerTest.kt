package com.github.niuhf0452.exile.inject

import com.github.niuhf0452.exile.inject.impl.ByteBuddyEnhancer
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.string.shouldBeEmpty
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation

class ByteBuddyTest : EnhancerTest(ByteBuddyEnhancer())

abstract class EnhancerTest(
        private val enhancer: ClassEnhancer
) : FunSpec({
    test("An Injector should work without interceptors") {
        @Inject
        class TestClass {
            fun foo() = "bar"
        }

        val injector = Injector.builder()
                .addPackage(EnhancerTest::class.java.packageName)
                .enhancer(enhancer)
                .build()
        val obj = injector.getInstance(TestClass::class)
        obj.foo() shouldBe "bar"
    }

    test("An Injector should work with interceptors") {
        @Inject
        class TestClass {
            fun foo() = "bar"
        }

        var called = false
        var state0 = ""
        val injector = Injector.builder()
                .addPackage(EnhancerTest::class.java.packageName)
                .enhancer(enhancer)
                .addInterceptor(object : ClassInterceptor {
                    override fun intercept(method: KFunction<*>): MethodInterceptor<*, *>? {
                        return object : MethodInterceptor<Any, String> {
                            override fun beforeCall(instance: Any, args: List<Any?>): String {
                                called = true
                                return "state"
                            }

                            override fun afterCall(instance: Any, args: List<Any?>, exception: Exception?,
                                                   returnValue: Any?, state: String) {
                                state0 = state
                            }
                        }
                    }
                })
                .build()
        val obj = injector.getInstance(TestClass::class)
        called.shouldBeFalse()
        state0.shouldBeEmpty()
        obj.foo() shouldBe "bar"
        called.shouldBeTrue()
        state0 shouldBe "state"
    }

    test("The interceptors should be called in order") {
        @Inject
        class TestClass {
            fun foo() = "bar"
        }

        val callOrder = mutableListOf<String>()

        class Interceptor(private val name: String) : ClassInterceptor {
            override fun intercept(method: KFunction<*>): MethodInterceptor<*, *>? {
                return object : MethodInterceptor<Any, Unit> {
                    override fun beforeCall(instance: Any, args: List<Any?>) {
                        callOrder.add(name)
                    }

                    override fun afterCall(instance: Any, args: List<Any?>, exception: Exception?,
                                           returnValue: Any?, state: Unit) {
                        callOrder.add(name)
                    }
                }
            }
        }

        val injector = Injector.builder()
                .addPackage(EnhancerTest::class.java.packageName)
                .enhancer(enhancer)
                .addInterceptor(Interceptor("a"))
                .addInterceptor(Interceptor("b"))
                .build()
        val obj = injector.getInstance(TestClass::class)
        callOrder.shouldBeEmpty()
        obj.foo() shouldBe "bar"
        callOrder shouldBe listOf("a", "b", "b", "a")
    }

    test("The enhancer should keep annotations") {
        @Inject
        class TestClass {
            @TestAnnotation
            fun foo() = "bar"
        }

        class Interceptor : ClassInterceptor {
            override fun intercept(method: KFunction<*>): MethodInterceptor<*, *>? {
                return object : MethodInterceptor<Any, Unit> {
                    override fun beforeCall(instance: Any, args: List<Any?>) = Unit

                    override fun afterCall(instance: Any, args: List<Any?>, exception: Exception?,
                                           returnValue: Any?, state: Unit) = Unit
                }
            }
        }

        val injector = Injector.builder()
                .addPackage(EnhancerTest::class.java.packageName)
                .enhancer(enhancer)
                .addInterceptor(Interceptor())
                .build()
        val obj = injector.getInstance(TestClass::class)
        obj.foo() shouldBe "bar"
        obj::foo.findAnnotation<TestAnnotation>().shouldNotBeNull()
    }

    test("The Injector should only intercept method in certain package") {
        @Inject
        class TestClass {
            fun foo() = "bar"
        }

        val intercepted = mutableListOf<String>()

        class Interceptor : ClassInterceptor {
            override fun intercept(method: KFunction<*>): MethodInterceptor<*, *>? {
                intercepted.add(method.name)
                return object : MethodInterceptor<Any, Unit> {
                    override fun beforeCall(instance: Any, args: List<Any?>) = Unit

                    override fun afterCall(instance: Any, args: List<Any?>, exception: Exception?, returnValue: Any?, state: Unit) = Unit
                }
            }
        }

        val injector = Injector.builder()
                .addPackage(EnhancerTest::class.java.packageName)
                .enhancer(enhancer)
                .addInterceptor(Interceptor())
                .build()
        val obj = injector.getInstance(TestClass::class)
        obj.foo() shouldBe "bar"
        intercepted shouldBe listOf("foo")
    }
}) {
    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class TestAnnotation
}