package io.github.niuhf0452.exile.test.factory0

import io.github.niuhf0452.exile.inject.Factory

interface AutowireTestInterfaceA

@Factory
class TestClassAFactory {
    @Factory
    fun get(): AutowireTestInterfaceA {
        return object : AutowireTestInterfaceA {}
    }
}