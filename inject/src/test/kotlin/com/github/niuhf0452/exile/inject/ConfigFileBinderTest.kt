package com.github.niuhf0452.exile.inject

import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec

class ConfigFileBinderTest : FunSpec({
    test("A ConfigFileBuilder should load mapping from config file") {
        val injector = Injector.builder()
                .addPackage(ConfigFileBinderTest::class.java.packageName)
                .enableConfigFile()
                .build()
        val bindings = injector.getBindings(TypeKey(Test::class)).toList()
        bindings.size shouldBe 2
        bindings[0].getInstance().shouldBeInstanceOf<TestClass>()
        bindings[1].getInstance() shouldBe TestClass2
    }
}) {
    interface Test

    @Inject
    class TestClass : Test

    @Inject
    object TestClass2 : Test
}