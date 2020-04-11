package com.github.niuhf0452.exile.yaml

import com.github.niuhf0452.exile.web.MediaType
import io.kotlintest.matchers.collections.shouldContain
import io.kotlintest.matchers.string.shouldMatch
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.shouldBe
import io.kotlintest.specs.FunSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.EmptyModule

class YamlEntitySerializerTest : FunSpec({
    test("A YAML serializer should support YAML media types") {
        val serializer = YamlEntitySerializer.Factory().createSerializer(EmptyModule)
        serializer.mediaTypes shouldContain MediaType.APPLICATION_X_YAML
        serializer.mediaTypes shouldContain MediaType.TEXT_YAML
    }

    test("A YAML serializer should serialize object to YAML") {
        @Serializable
        data class Test(val i: Int)

        val serializer = YamlEntitySerializer.Factory().createSerializer(EmptyModule)
        val buf = serializer.serialize(Test(2), MediaType.TEXT_YAML)
        buf.toString(Charsets.UTF_8) shouldMatch "i:\\s+2"
    }

    test("A YAML serializer should deserialize YAML to object") {
        @Serializable
        data class Test(val i: Int)

        val serializer = YamlEntitySerializer.Factory().createSerializer(EmptyModule)
        val obj = serializer.deserialize("i: 100".toByteArray(), Test::class, MediaType.TEXT_YAML)
        obj.shouldBeInstanceOf<Test>()
        (obj as Test).i shouldBe 100
    }
})