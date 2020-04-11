package com.github.niuhf0452.exile.common

import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec

class OrderTest : FunSpec({
    test("Orders should respect annotation") {
        @Order(1)
        class Test

        Orders.getOrder(Test::class) shouldBe 1
        Orders.getOrder(Test()) shouldBe 1
    }

    test("Orders should respect Ordered") {
        @Order(1)
        class Test : Ordered {
            override fun getOrder(): Int = 2
        }

        Orders.getOrder(Test::class) shouldBe 1
        Orders.getOrder(Test()) shouldBe 2
    }

    test("Orders should provide comparator") {
        @Order(Orders.DEFAULT)
        class Test1

        @Order(Orders.DEFAULT + 2)
        class Test2

        val arr = mutableListOf(Test2(), Test1())
        arr[0].shouldBeInstanceOf<Test2>()
        arr[1].shouldBeInstanceOf<Test1>()
        arr.sortWith(Orders.comparator())
        arr[0].shouldBeInstanceOf<Test1>()
        arr[1].shouldBeInstanceOf<Test2>()
    }
})