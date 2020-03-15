package com.github.niuhf0452.exile.config

import com.github.niuhf0452.exile.config.impl.CompositeSource
import com.github.niuhf0452.exile.config.impl.SimpleConfigSource
import io.kotlintest.matchers.string.shouldStartWith
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec

class CompositeSourceTest : FunSpec({
    test("A CompositeSource should add sources") {
        val composite = CompositeSource.newSource()
        val source = SimpleConfigSource("test=1")
        composite.addSource(source)
        composite.addSource(source)
        composite.toList() shouldBe listOf(source, source)
    }

    test("A CompositeSource should implement toString") {
        CompositeSource.newSource().toString() shouldStartWith "CompositeSource"
    }
})