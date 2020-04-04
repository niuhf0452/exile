package com.github.niuhf0452.exile.common

import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.FunSpec
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class DurationTest : FunSpec({
    test("Duration should parse string") {
        Duration.parse("10 ns") shouldBe Duration(10, TimeUnit.NANOSECONDS)
        Duration.parse("10nanosecond") shouldBe Duration(10, TimeUnit.NANOSECONDS)
        Duration.parse("10 nanoseconds") shouldBe Duration(10, TimeUnit.NANOSECONDS)

        Duration.parse("10 us") shouldBe Duration(10, TimeUnit.MICROSECONDS)
        Duration.parse("10 microsecond") shouldBe Duration(10, TimeUnit.MICROSECONDS)
        Duration.parse("10 microseconds") shouldBe Duration(10, TimeUnit.MICROSECONDS)

        Duration.parse("10 ms") shouldBe Duration(10, TimeUnit.MILLISECONDS)
        Duration.parse("10 millisecond") shouldBe Duration(10, TimeUnit.MILLISECONDS)
        Duration.parse("10 milliseconds") shouldBe Duration(10, TimeUnit.MILLISECONDS)

        Duration.parse("10 S") shouldBe Duration(10, TimeUnit.SECONDS)
        Duration.parse("10sec") shouldBe Duration(10, TimeUnit.SECONDS)
        Duration.parse("10 second") shouldBe Duration(10, TimeUnit.SECONDS)
        Duration.parse("10 seconds") shouldBe Duration(10, TimeUnit.SECONDS)

        Duration.parse("10h") shouldBe Duration(10, TimeUnit.HOURS)
        Duration.parse("10 hour") shouldBe Duration(10, TimeUnit.HOURS)
        Duration.parse("10 hours") shouldBe Duration(10, TimeUnit.HOURS)

        Duration.parse("10D") shouldBe Duration(10, TimeUnit.DAYS)
        Duration.parse("10 day") shouldBe Duration(10, TimeUnit.DAYS)
        Duration.parse("10 days") shouldBe Duration(10, TimeUnit.DAYS)
    }

    test("Duration should throw if the format is invalid") {
        shouldThrow<IllegalArgumentException> {
            Duration.parse("19 ppi")
        }

        shouldThrow<IllegalArgumentException> {
            Duration.parse("19.3 hour")
        }
    }

    test("A Duration should convert unit") {
        Duration(1, TimeUnit.DAYS).toUnit(TimeUnit.HOURS).amount shouldBe 24
        Duration(1, TimeUnit.HOURS).toUnit(TimeUnit.MINUTES).amount shouldBe 60
        Duration(1, TimeUnit.MINUTES).toUnit(TimeUnit.SECONDS).amount shouldBe 60
        Duration(1, TimeUnit.SECONDS).toUnit(TimeUnit.MILLISECONDS).amount shouldBe 1000
        Duration(1, TimeUnit.MILLISECONDS).toUnit(TimeUnit.MICROSECONDS).amount shouldBe 1000
        Duration(1, TimeUnit.MICROSECONDS).toUnit(TimeUnit.NANOSECONDS).amount shouldBe 1000
    }

    test("A Duration should be comparable") {
        Duration(1, TimeUnit.DAYS) shouldBe Duration(24, TimeUnit.HOURS)
        (Duration(1, TimeUnit.DAYS) > Duration(1, TimeUnit.HOURS)).shouldBeTrue()
    }

    test("Duration should implement equals, hashCode and toString") {
        Duration(1, TimeUnit.DAYS) shouldBe Duration(1, TimeUnit.DAYS)
        Duration(1, TimeUnit.DAYS).hashCode() shouldBe Duration(1, TimeUnit.DAYS).hashCode()
        Duration(1, TimeUnit.DAYS).toString() shouldBe Duration(1, TimeUnit.DAYS).toString()
    }

    test("Duration should convert to Java duration") {
        val duration = Duration(1, TimeUnit.DAYS)
        val javaDuration = duration.toJavaDuration()
        duration.toUnit(TimeUnit.SECONDS).amount shouldBe javaDuration.seconds
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    test("A Duration should be serialized") {
        val obj = Duration(10, TimeUnit.SECONDS)
        val json = Json.stringify(Duration.serializer(), obj)
        val obj2 = Json.parse(Duration.serializer(), json)
        obj2 shouldBe obj
        obj2.amount shouldBe obj.amount
        obj2.unit shouldBe obj.unit
    }
})