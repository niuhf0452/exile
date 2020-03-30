package com.github.niuhf0452.exile.reflection

import com.github.niuhf0452.exile.reflection.internal.JdkProxyFactoryBuilder
import io.kotlintest.matchers.beInstanceOf
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FunSpec
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty

class JdkProxyTest : ProxyTest() {
    override fun <A> newBuilder(): ProxyFactoryBuilder<A> {
        return JdkProxyFactoryBuilder()
    }
}

abstract class ProxyTest : FunSpec() {
    protected abstract fun <A> newBuilder(): ProxyFactoryBuilder<A>

    init {
        test("A proxy should accept state") {
            val methods = mutableListOf<KFunction<*>>()
            val factory = newBuilder<String>()
                    .addInterface(Test1::class)
                    .handle { m ->
                        methods.add(m)
                        object : ProxyMethodHandler<String> {
                            override fun call(state: String, instance: Any, args: Array<out Any?>?): Any? {
                                return state
                            }
                        }
                    }
                    .build()
            methods shouldBe listOf(Test1::echo)
            val obj = factory.createObject("foo")
            obj should beInstanceOf<Test1>()
            (obj as Test1).echo() shouldBe "foo"
        }

        test("A proxy method should accept parameters") {
            val factory = newBuilder<Unit>()
                    .addInterface(Test2::class)
                    .handle {
                        object : ProxyMethodHandler<Unit> {
                            override fun call(state: Unit, instance: Any, args: Array<out Any?>?): Any? {
                                return args!![0]
                            }
                        }
                    }
                    .build()
            val obj = factory.createObject(Unit)
            obj should beInstanceOf<Test2>()
            (obj as Test2).echo("foo") shouldBe "foo"
        }

        test("A proxy should call default implementation") {
            val factory = newBuilder<Unit>()
                    .addInterface(Test3::class)
                    .filter { false }
                    .build()
            val obj = factory.createObject(Unit) as Test3
            obj.foo() shouldBe "foo"
            obj.bar() shouldBe "bar"
        }

        test("A builder should respect filter") {
            val methods = mutableListOf<KFunction<*>>()
            val factory = newBuilder<Unit>()
                    .addInterface(Test3::class)
                    .filter { m -> m.name == "bar" }
                    .handle {
                        methods.add(it)
                        ProxyHandlers.returnValue("hello")
                    }
                    .build()
            methods shouldBe listOf(Test3::bar)
            val obj = factory.createObject(Unit) as Test3
            obj.foo() shouldBe "foo"
            obj.bar() shouldBe "hello"
        }

        test("A proxy should handle getter") {
            val factory = newBuilder<Unit>()
                    .addInterface(Test4::class)
                    .handle {
                        ProxyHandlers.returnValue(1)
                    }
                    .build()
            val obj = factory.createObject(Unit) as Test4
            obj.value shouldBe 1
        }

        test("A proxy should handle setter") {
            val factory = newBuilder<AtomicInteger>()
                    .addInterface(Test5::class)
                    .handle { m ->
                        if (m is KProperty.Getter) {
                            object : ProxyMethodHandler<AtomicInteger> {
                                override fun call(state: AtomicInteger, instance: Any, args: Array<out Any?>?): Any? {
                                    return state.get()
                                }
                            }
                        } else {
                            object : ProxyMethodHandler<AtomicInteger> {
                                override fun call(state: AtomicInteger, instance: Any, args: Array<out Any?>?): Any? {
                                    state.set(args!![0] as Int)
                                    return null
                                }
                            }
                        }
                    }
                    .build()
            val obj = factory.createObject(AtomicInteger(0)) as Test5
            obj.value shouldBe 0
            obj.value = 1
            obj.value shouldBe 1
        }

        test("A proxy should call java default method") {
            val factory = newBuilder<Unit>()
                    .addInterface(Test6::class)
                    .filter { false }
                    .build()
            val obj = factory.createObject(Unit) as Test6
            obj.hello() shouldBe "hello"
        }

        test("A proxy should throw if input class is not interface") {
            shouldThrow<IllegalArgumentException> {
                newBuilder<Unit>()
                        .addInterface(Object::class)
                        .filter { false }
                        .build()
            }
        }

        test("A proxy should throw if handle method with default handler") {
            val factory = newBuilder<Unit>()
                    .addInterface(Test1::class)
                    .build()
            val obj = factory.createObject(Unit) as Test1
            shouldThrow<UnsupportedOperationException> {
                obj.echo()
            }
        }

        test("A proxy should throw if method is not handled") {
            shouldThrow<IllegalArgumentException> {
                newBuilder<Unit>()
                        .addInterface(Test1::class)
                        .filter { false }
                        .build()
            }
        }

        test("A proxy should handle equals, hashCode and toString") {
            val factory = newBuilder<Unit>()
                    .addInterface(Test1::class)
                    .build()
            val a = factory.createObject(Unit)
            val b = factory.createObject(Unit)
            a shouldBe a
            b shouldBe b
            a shouldNotBe b
            a.hashCode() shouldNotBe b.hashCode()
            a.toString() shouldBe b.toString()
        }
    }

    interface Test1 {
        fun echo(): String
    }

    interface Test2 {
        fun echo(value: String): String
    }

    interface Test3 {
        fun foo(): String {
            return "foo"
        }

        fun bar(): String {
            return "bar"
        }
    }

    interface Test4 {
        val value: Int
    }

    interface Test5 {
        var value: Int
    }

    interface Test6 {
        @JvmDefault
        fun hello(): String {
            return "hello"
        }
    }
}