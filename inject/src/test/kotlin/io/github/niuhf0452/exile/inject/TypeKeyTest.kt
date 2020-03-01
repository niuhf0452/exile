package io.github.niuhf0452.exile.inject

import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FeatureSpec
import kotlin.reflect.KClass

class TypeKeyTest : FeatureSpec({
    feature("create a TypeKey") {
        scenario("create a TypeKey from class") {
            val key = TypeKey(String::class)
            key.classifier shouldBe String::class
            key.arguments.shouldBeEmpty()
        }

        scenario("create a TypeKey from generic class") {
            val key1 = TypeKey(object : TypeLiteral<Pair<Int, String>>() {}.type)
            val key2 = TypeKey(Pair::class, listOf(TypeKey(Int::class), TypeKey(String::class)))
            key1 shouldBe key2
            key1.classifier shouldBe Pair::class
            key1.arguments.size shouldBe 2
            key1.arguments[0].classifier shouldBe Int::class
            key1.arguments[0].arguments.shouldBeEmpty()
            key1.arguments[1].classifier shouldBe String::class
            key1.arguments[1].arguments.shouldBeEmpty()
        }

        scenario("A TypeKey should NOT accept star type") {
            shouldThrow<IllegalArgumentException> {
                TypeKey(object : TypeLiteral<Function<*>>() {}.type)
            }
        }

        scenario("A TypeKey should NOT accept variant type") {
            shouldThrow<IllegalArgumentException> {
                TypeKey(object : TypeLiteral<KClass<out Any>>() {}.type)
            }
        }

        scenario("A TypeKey should NOT accept variant type in nested") {
            shouldThrow<IllegalArgumentException> {
                TypeKey(object : TypeLiteral<Function<KClass<out Any>>>() {}.type)
            }
        }
    }

    feature("equality") {
        scenario("A TypeKey should equal to itself") {
            val key = TypeKey(String::class)
            key shouldBe key
        }

        scenario("A TypeKey should equal to another TypeKey created from same type") {
            TypeKey(String::class) shouldBe TypeKey(String::class)
        }

        scenario("A TypeKey should equal to another TypeKey created from same generic type") {
            TypeKey(object : TypeLiteral<Function<KClass<Any>>>() {}.type) shouldBe
                    TypeKey(object : TypeLiteral<Function<KClass<Any>>>() {}.type)
        }

        scenario("A TypeKey should NOT equal to another TypeKey created with different type") {
            TypeKey(String::class) shouldNotBe TypeKey(Int::class)
        }

        scenario("A TypeKey should NOT equal to another TypeKey created with different parameter type") {
            TypeKey(object : TypeLiteral<Function<KClass<Any>>>() {}.type) shouldNotBe
                    TypeKey(object : TypeLiteral<Function<KClass<String>>>() {}.type)
        }
    }

    feature("check type compliance") {
        scenario("A TypeKey should comply to itself") {
            val key = TypeKey(String::class)
            key.isAssignableFrom(key).shouldBeTrue()
        }

        scenario("A TypeKey should comply to a super class") {
            abstract class Super
            class TestClass : Super()

            TypeKey(Super::class).isAssignableFrom(TypeKey(TestClass::class)).shouldBeTrue()
        }

        scenario("A TypeKey should comply to a generic super class") {
            abstract class Super<A>
            class TestClass : Super<String>()

            TypeKey(object : TypeLiteral<Super<String>>() {}.type)
                    .isAssignableFrom(TypeKey(TestClass::class)).shouldBeTrue()
        }

        scenario("A TypeKey of generic type should comply to a generic super class") {
            abstract class Super<A>
            class TestClass<A> : Super<A>()

            TypeKey(object : TypeLiteral<Super<String>>() {}.type)
                    .isAssignableFrom(TypeKey(object : TypeLiteral<TestClass<String>>() {}.type))
                    .shouldBeTrue()
        }

        scenario("A TypeKey should NOT comply to a non-super class") {
            TypeKey(String::class).isAssignableFrom(TypeKey(Int::class)).shouldBeFalse()
        }

        scenario("A TypeKey should NOT comply to a generic super class with different parameter type") {
            abstract class Super<A>
            class TestClass : Super<String>()

            TypeKey(object : TypeLiteral<Super<Int>>() {}.type)
                    .isAssignableFrom(TypeKey(TestClass::class)).shouldBeFalse()
        }

        scenario("A TypeKey of generic type should NOT comply to a generic super class " +
                "with different parameter type") {
            abstract class Super<A>
            class TestClass<A> : Super<A>()

            TypeKey(object : TypeLiteral<Super<Int>>() {}.type)
                    .isAssignableFrom(TypeKey(object : TypeLiteral<TestClass<String>>() {}.type)).shouldBeFalse()
        }
    }
})