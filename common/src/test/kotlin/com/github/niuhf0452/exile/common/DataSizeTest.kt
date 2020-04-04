package com.github.niuhf0452.exile.common

import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FunSpec
import kotlinx.serialization.json.Json

class DataSizeTest : FunSpec({
    test("DataSize should parse string") {
        DataSize.parse("10 b") shouldBe DataSize(10, DataSize.Unit.BYTE)
        DataSize.parse("10b") shouldBe DataSize(10, DataSize.Unit.BYTE)
        DataSize.parse("10 byte") shouldBe DataSize(10, DataSize.Unit.BYTE)
        DataSize.parse("10 bytes") shouldBe DataSize(10, DataSize.Unit.BYTE)

        DataSize.parse("10 k") shouldBe DataSize(10, DataSize.Unit.Ki)
        DataSize.parse("10kb") shouldBe DataSize(10, DataSize.Unit.Ki)
        DataSize.parse("10 kilobyte") shouldBe DataSize(10, DataSize.Unit.Ki)
        DataSize.parse("10 kilobytes") shouldBe DataSize(10, DataSize.Unit.Ki)
        DataSize.parse("10Ki") shouldBe DataSize(10, DataSize.Unit.Ki)
        DataSize.parse("10 KiB") shouldBe DataSize(10, DataSize.Unit.Ki)
        DataSize.parse("10 kibibyte") shouldBe DataSize(10, DataSize.Unit.Ki)
        DataSize.parse("10 kibibytes") shouldBe DataSize(10, DataSize.Unit.Ki)

        DataSize.parse("10 m") shouldBe DataSize(10, DataSize.Unit.Mi)
        DataSize.parse("10mb") shouldBe DataSize(10, DataSize.Unit.Mi)
        DataSize.parse("10 megabyte") shouldBe DataSize(10, DataSize.Unit.Mi)
        DataSize.parse("10 megabytes") shouldBe DataSize(10, DataSize.Unit.Mi)
        DataSize.parse("10mi") shouldBe DataSize(10, DataSize.Unit.Mi)
        DataSize.parse("10 MiB") shouldBe DataSize(10, DataSize.Unit.Mi)
        DataSize.parse("10 mebibyte") shouldBe DataSize(10, DataSize.Unit.Mi)
        DataSize.parse("10 mebibytes") shouldBe DataSize(10, DataSize.Unit.Mi)

        DataSize.parse("10 g") shouldBe DataSize(10, DataSize.Unit.Gi)
        DataSize.parse("10GB") shouldBe DataSize(10, DataSize.Unit.Gi)
        DataSize.parse("10 gigabyte") shouldBe DataSize(10, DataSize.Unit.Gi)
        DataSize.parse("10 gigabytes") shouldBe DataSize(10, DataSize.Unit.Gi)
        DataSize.parse("10gi") shouldBe DataSize(10, DataSize.Unit.Gi)
        DataSize.parse("10 GiB") shouldBe DataSize(10, DataSize.Unit.Gi)
        DataSize.parse("10 gibibyte") shouldBe DataSize(10, DataSize.Unit.Gi)
        DataSize.parse("10 gibibytes") shouldBe DataSize(10, DataSize.Unit.Gi)

        DataSize.parse("10 t") shouldBe DataSize(10, DataSize.Unit.Ti)
        DataSize.parse("10TB") shouldBe DataSize(10, DataSize.Unit.Ti)
        DataSize.parse("10 terabyte") shouldBe DataSize(10, DataSize.Unit.Ti)
        DataSize.parse("10 terabytes") shouldBe DataSize(10, DataSize.Unit.Ti)
        DataSize.parse("10ti") shouldBe DataSize(10, DataSize.Unit.Ti)
        DataSize.parse("10 TiB") shouldBe DataSize(10, DataSize.Unit.Ti)
        DataSize.parse("10 tebibyte") shouldBe DataSize(10, DataSize.Unit.Ti)
        DataSize.parse("10 tebibytes") shouldBe DataSize(10, DataSize.Unit.Ti)

        DataSize.parse("10 p") shouldBe DataSize(10, DataSize.Unit.Pi)
        DataSize.parse("10PB") shouldBe DataSize(10, DataSize.Unit.Pi)
        DataSize.parse("10 petabyte") shouldBe DataSize(10, DataSize.Unit.Pi)
        DataSize.parse("10 petabytes") shouldBe DataSize(10, DataSize.Unit.Pi)
        DataSize.parse("10pi") shouldBe DataSize(10, DataSize.Unit.Pi)
        DataSize.parse("10 PiB") shouldBe DataSize(10, DataSize.Unit.Pi)
        DataSize.parse("10 pebibyte") shouldBe DataSize(10, DataSize.Unit.Pi)
        DataSize.parse("10 pebibytes") shouldBe DataSize(10, DataSize.Unit.Pi)
    }

    test("DataSize should throw if the format is invalid") {
        shouldThrow<IllegalArgumentException> {
            DataSize.parse("19 ppi")
        }

        shouldThrow<IllegalArgumentException> {
            DataSize.parse("19.3 mb")
        }
    }

    test("A DataSize should convert unit") {
        DataSize(1, DataSize.Unit.Pi).toUnit(DataSize.Unit.Ti).amount shouldBe 1024
        DataSize(1, DataSize.Unit.Ti).toUnit(DataSize.Unit.Gi).amount shouldBe 1024
        DataSize(1, DataSize.Unit.Gi).toUnit(DataSize.Unit.Mi).amount shouldBe 1024
        DataSize(1, DataSize.Unit.Mi).toUnit(DataSize.Unit.Ki).amount shouldBe 1024
        DataSize(1, DataSize.Unit.Ki).toUnit(DataSize.Unit.BYTE).amount shouldBe 1024
    }

    test("A DataSize should be comparable") {
        DataSize(1, DataSize.Unit.Pi) shouldBe DataSize(1024, DataSize.Unit.Ti)
        (DataSize(1, DataSize.Unit.Pi) > DataSize(1, DataSize.Unit.Ti)).shouldBeTrue()
    }

    test("DataSize should implement equals, hashCode and toString") {
        DataSize(1, DataSize.Unit.Ki) shouldBe DataSize(1, DataSize.Unit.Ki)
        DataSize(1, DataSize.Unit.Ki).hashCode() shouldBe DataSize(1, DataSize.Unit.Ki).hashCode()
        DataSize(1, DataSize.Unit.Ki).toString() shouldBe DataSize(1, DataSize.Unit.Ki).toString()
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    test("A DataSize should be serialized") {
        val obj = DataSize(10, DataSize.Unit.Ki)
        val json = Json.stringify(DataSize.serializer(), obj)
        val obj2 = Json.parse(DataSize.serializer(), json)
        obj2 shouldBe obj
        obj2.amount shouldBe obj.amount
        obj2.unit shouldBe obj.unit
    }
})