package com.github.niuhf0452.exile.inject

import com.github.niuhf0452.exile.test.autoconfigure.Test
import com.github.niuhf0452.exile.test.autoconfigure.TestClass
import io.kotlintest.matchers.beInstanceOf
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec

class AutoConfigureTest : FunSpec({
    test("A builder should auto configure") {
        val injector = Injector.builder()
                .autoConfigure(Test::class.java.packageName)
                .build()
        injector.getInstance(Test::class) should beInstanceOf<TestClass>()
        injector.getInstance(Test2::class) shouldBe TestObj
    }
}) {
    interface Test2

    object TestObj : Test2

    class TestLoader : InjectorAutoLoader {
        override fun getBinders(): List<Injector.Binder> {
            return listOf(object : Injector.Binder {
                override fun bind(key: TypeKey, context: Injector.BindingContext) {
                    if (key == TypeKey(Test2::class)) {
                        context.bindToInstance(emptyList(), TestObj)
                    }
                }
            })
        }
    }
}