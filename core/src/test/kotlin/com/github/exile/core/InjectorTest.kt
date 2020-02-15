package com.github.exile.core

import com.github.exile.core.impl.StaticBinder
import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec

class InjectorTest : FeatureSpec({
    feature("static binder") {
        scenario("bind to instance") {
            val injector = Injector.builder()
                    .addBinder(StaticBinder { c ->
                        c.bind(String::class).toInstance("hello, world")
                    })
                    .build()
            injector.getInstance(String::class) shouldBe "hello, world"
        }

        scenario("bind to instance with qualifier") {
            val injector = Injector.builder()
                    .addBinder(StaticBinder { c ->
                        c.bind(String::class).qualified("foo").toInstance("bar")
                    })
                    .build()
            injector.getInstance(String::class, "foo") shouldBe "bar"
        }

        scenario("bind to multiple instances with qualifier") {
            val injector = Injector.builder()
                    .addBinder(StaticBinder { c ->
                        c.bind(String::class).qualified("foo").toInstance("bar")
                        c.bind(String::class).qualified("hello").toInstance("world")
                    })
                    .build()
            injector.getInstance(String::class, "foo") shouldBe "bar"
            injector.getInstance(String::class, "hello") shouldBe "world"
        }
    }
})

