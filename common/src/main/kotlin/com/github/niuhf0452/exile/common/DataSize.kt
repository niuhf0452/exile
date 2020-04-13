package com.github.niuhf0452.exile.common

import kotlinx.serialization.*
import kotlin.reflect.jvm.jvmName

@PublicApi
@Serializable(with = DataSize.Serializer::class)
class DataSize(val amount: Long, val unit: Unit) : Comparable<DataSize> {
    private val bytes = Unit.BYTE.convertFrom(amount, unit)

    enum class Unit(
            private val scalarToBytes: Long
    ) {
        BYTE(1),
        Ki(1024 * BYTE.scalarToBytes),
        Mi(1024 * Ki.scalarToBytes),
        Gi(1024 * Mi.scalarToBytes),
        Ti(1024 * Gi.scalarToBytes),
        Pi(1024 * Ti.scalarToBytes);

        fun convertFrom(amount: Long, unit: Unit): Long {
            return amount * unit.scalarToBytes / scalarToBytes
        }
    }

    override fun toString(): String {
        val u = when (unit) {
            Unit.BYTE -> "B"
            Unit.Ki -> "Ki"
            Unit.Mi -> "Mi"
            Unit.Gi -> "Gi"
            Unit.Ti -> "Ti"
            Unit.Pi -> "Pi"
        }
        return "$amount $u"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return bytes == (other as DataSize).bytes
    }

    override fun hashCode(): Int {
        return bytes.hashCode()
    }

    override fun compareTo(other: DataSize): Int {
        return bytes.compareTo(other.bytes)
    }

    fun toUnit(unit: Unit): DataSize {
        return DataSize(unit.convertFrom(amount, this.unit), unit)
    }

    fun inBytes(): Long {
        return bytes
    }

    companion object {
        private val pattern = "(\\d+)\\s*(\\w+)".toRegex()

        fun parse(value: String): DataSize {
            val m = pattern.matchEntire(value)
                    ?: throw IllegalArgumentException("Invalid data size format: $value")
            val amount = m.groupValues[1].toLong()
            val unit = when (m.groupValues[2].toLowerCase()) {
                "b", "byte", "bytes" -> Unit.BYTE
                "k", "kb", "kilobyte", "kilobytes", "ki", "kib", "kibibyte", "kibibytes" -> Unit.Ki
                "m", "mb", "megabyte", "megabytes", "mi", "mib", "mebibyte", "mebibytes" -> Unit.Mi
                "g", "gb", "gigabyte", "gigabytes", "gi", "gib", "gibibyte", "gibibytes" -> Unit.Gi
                "t", "tb", "terabyte", "terabytes", "ti", "tib", "tebibyte", "tebibytes" -> Unit.Ti
                "p", "pb", "petabyte", "petabytes", "pi", "pib", "pebibyte", "pebibytes" -> Unit.Pi
                else -> throw IllegalArgumentException("Invalid data size format: $value")
            }
            return DataSize(amount, unit)
        }
    }

    class Serializer : KSerializer<DataSize> {
        override val descriptor: SerialDescriptor = PrimitiveDescriptor(DataSize::class.jvmName, PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): DataSize {
            return parse(decoder.decodeString())
        }

        override fun serialize(encoder: Encoder, value: DataSize) {
            encoder.encodeString(value.toString())
        }
    }
}