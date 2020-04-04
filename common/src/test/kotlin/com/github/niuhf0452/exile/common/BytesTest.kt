package com.github.niuhf0452.exile.common

import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec

class BytesTest : FunSpec({
    test("Bytes should convert ByteArray and hex") {
        Bytes.toHex(Bytes.fromHex("a0c2")) shouldBe "a0c2"
    }
})