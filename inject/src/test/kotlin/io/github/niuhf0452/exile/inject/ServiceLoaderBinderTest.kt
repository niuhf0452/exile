package io.github.niuhf0452.exile.inject

import io.kotlintest.matchers.beInstanceOf
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec

class ServiceLoaderBinderTest : FunSpec({
    val injector = Injector.builder()
            .addPackage(ServiceLoaderBinderTest::class.java.packageName)
            .enableServiceLoader()
            .build()

    test("A ServiceLoaderBinder should inject services") {
        val list = injector.getBindings(TypeKey(TestService::class)).toList()
        list.size shouldBe 2
        list[0].getInstance() should beInstanceOf<FooService>()
        list[1].getInstance() should beInstanceOf<BarService>()
    }
}) {
    interface TestService {
        fun getMessage(): String
    }

    class FooService : TestService {
        override fun getMessage(): String {
            return "foo"
        }
    }

    class BarService : TestService {
        override fun getMessage(): String {
            return "bar"
        }
    }
}
