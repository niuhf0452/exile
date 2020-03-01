package io.github.niuhf0452.exile.test.factory1

import io.github.niuhf0452.exile.inject.Factory

@Factory
class TestClass {
    @Factory
    fun <A> get(): String {
        return "foo"
    }
}