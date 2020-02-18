package com.github.exile.inject

import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FunSpec
import kotlin.reflect.KVariance

class TypeLiteralTest : FunSpec({
    test("A TypeLiteral should accept class type") {
        val type = object : TypeLiteral<String>() {}.type
        type.classifier shouldBe String::class
    }

    test("A TypeLiteral should accept specific generic List type") {
        val type = object : TypeLiteral<List<String>>() {}.type
        type.classifier shouldBe List::class
        type.arguments.first().type?.classifier shouldBe String::class
        type.arguments.first().variance shouldBe KVariance.INVARIANT
    }

    test("A TypeLiteral should accept unspecific generic List type") {
        val type = object : TypeLiteral<List<*>>() {}.type
        type.classifier shouldBe List::class
        type.arguments.first().type shouldBe null
        type.arguments.first().variance shouldBe null
    }

    test("A TypeLiteral should not accept indirect inheritance") {
        abstract class Parent : TypeLiteral<String>()
        class Child : Parent()

        shouldThrow<IllegalStateException> {
            Child().type
        }
    }

    test("A TypeLiteral should not accept parameter type") {
        class Generic<A> : TypeLiteral<A>()

        shouldThrow<IllegalStateException> {
            Generic<String>().type
        }
    }
})