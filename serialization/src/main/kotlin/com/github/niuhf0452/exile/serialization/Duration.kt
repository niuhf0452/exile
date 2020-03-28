package com.github.niuhf0452.exile.serialization

import kotlinx.serialization.*
import java.util.concurrent.TimeUnit

@Serializable(with = Duration.Serializer::class)
class Duration(val amount: Long, val unit: TimeUnit) : Comparable<Duration> {
    private val nano = TimeUnit.NANOSECONDS.convert(amount, unit)

    override fun toString(): String {
        val u = when (unit) {
            TimeUnit.NANOSECONDS -> "ns"
            TimeUnit.MICROSECONDS -> "μs"
            TimeUnit.MILLISECONDS -> "ms"
            TimeUnit.SECONDS -> "s"
            TimeUnit.MINUTES -> "m"
            TimeUnit.HOURS -> "h"
            TimeUnit.DAYS -> "d"
        }
        return "$amount $u"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return nano == (other as Duration).nano
    }

    override fun hashCode(): Int {
        return nano.hashCode()
    }

    override fun compareTo(other: Duration): Int {
        return nano.compareTo(other.nano)
    }

    fun toUnit(unit: TimeUnit): Duration {
        return Duration(unit.convert(amount, this.unit), unit)
    }

    fun toJavaDuration(): java.time.Duration {
        return java.time.Duration.ofNanos(nano)
    }

    companion object {
        private val pattern = "(\\d+)\\s*(\\w+)".toRegex()

        fun parse(value: String): Duration {
            val m = pattern.matchEntire(value)
                    ?: throw IllegalArgumentException("Invalid duration format: $value")
            val amount = m.groupValues[1].toLong()
            val unit = when (m.groupValues[2].toLowerCase()) {
                "ns", "nanosecond", "nanoseconds" -> TimeUnit.NANOSECONDS
                "us", "microsecond", "microseconds" -> TimeUnit.MICROSECONDS
                "ms", "millisecond", "milliseconds" -> TimeUnit.MILLISECONDS
                "s", "sec", "second", "seconds" -> TimeUnit.SECONDS
                "m", "min", "minute", "minutes" -> TimeUnit.MINUTES
                "h", "hour", "hours" -> TimeUnit.HOURS
                "d", "day", "days" -> TimeUnit.DAYS
                else -> throw IllegalArgumentException("Invalid duration format: $value")
            }
            return Duration(amount, unit)
        }
    }

    class Serializer : KSerializer<Duration> {
        override val descriptor: SerialDescriptor = PrimitiveDescriptor("Duration", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): Duration {
            return parse(decoder.decodeString())
        }

        override fun serialize(encoder: Encoder, value: Duration) {
            encoder.encodeString(value.toString())
        }
    }
}