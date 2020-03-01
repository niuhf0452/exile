package io.github.niuhf0452.exile.inject

import io.kotlintest.matchers.beInstanceOf
import io.kotlintest.matchers.collections.shouldContain
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.matchers.types.shouldNotBeSameInstanceAs
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FunSpec
import java.util.function.Consumer

class InstantiateBinderTest : FunSpec({
    val injector = Injector.builder().enableScope().build()

    test("A InstantiateBinder should create instance") {
        @Inject
        class TestClass

        injector.getInstance(TestClass::class)
    }

    test("A InstantiateBinder should create instance with parameters") {
        @Inject
        class ParameterClass

        @Inject
        class TestClass(val p: ParameterClass)

        val obj = injector.getInstance(TestClass::class)
        obj should beInstanceOf<TestClass>()
        obj.p should beInstanceOf<ParameterClass>()
    }

    test("A InstantiateBinder should inject object") {
        injector.getInstance(TestObject::class) shouldBeSameInstanceAs TestObject
    }

    test("A InstantiateBinder should create instance of generic class") {
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

    test("A InstantiateBinder should always inject the same instance for class with @Singleton") {
        @Inject
        @Singleton
        class TestClass

        val a = injector.getInstance(TestClass::class)
        val b = injector.getInstance(TestClass::class)
        a shouldBeSameInstanceAs b
    }

    test("A InstantiateBinder should inject different instances for class without @Singleton") {
        @Inject
        class TestClass

        val a = injector.getInstance(TestClass::class)
        val b = injector.getInstance(TestClass::class)
        a shouldNotBeSameInstanceAs b
    }

    test("A InstantiateBinder should bring qualifiers to binding") {
        @Inject
        @Singleton
        @TestQualifier
        class TestClass

        val binding = injector.getBindings(TypeKey(TestClass::class)).getSingle()
        binding.qualifiers.size shouldBe 2
        binding.qualifiers shouldContain Qualifiers.qualifier(TestQualifier::class)
        binding.qualifiers shouldContain Qualifiers.qualifier(Singleton::class)
    }

    test("A InstantiateBinder should bring qualifiers to binding for object") {
        val binding = injector.getBindings(TypeKey(TestObject::class)).getSingle()
        binding.qualifiers.size shouldBe 1
        binding.qualifiers shouldContain Qualifiers.qualifier(TestQualifier::class)
    }

    test("A InstantiateBinder should inject instance depends on nullable parameter") {
        @Inject
        class TestClass(val value: String?)

        val a = injector.getInstance(TestClass::class)
        a.value.shouldBeNull()
    }

    test("A InstantiateBinder should NOT create instance of abstract class") {
        @Inject
        abstract class TestClass

        shouldThrow<IllegalStateException> {
            injector.getInstance(TestClass::class)
        }

        shouldThrow<IllegalStateException> {
            injector.getBindings(object : TypeLiteral<Consumer<String>>() {}.typeKey).getSingle()
        }
    }

    test("A InstantiateBinder should NOT create instance of class without @Inject") {
        class TestClass

        shouldThrow<IllegalStateException> {
            injector.getInstance(TestClass::class)
        }
    }
}) {
    @TestQualifier
    object TestObject

    @Qualifier
    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class TestQualifier
}
