package com.github.niuhf0452.exile.web.serialization

import com.github.niuhf0452.exile.web.FailureResponseException
import com.github.niuhf0452.exile.web.VariableTypeConverter
import kotlin.reflect.KClass

object VariableTypeConverters {
    private val convertersByType = mapOf(
            String::class to StringConverter(),
            Int::class to IntConverter(),
            Long::class to LongConverter(),
            Boolean::class to BooleanConverter(),
            Float::class to FloatConverter(),
            Double::class to DoubleConverter()
    )

    fun getConverter(cls: KClass<*>): VariableTypeConverter<*>? {
        return convertersByType[cls]
    }

    class StringConverter : VariableTypeConverter<String> {
        override fun parse(value: String): String {
            return value
        }

        override fun stringify(value: String): String {
            return value
        }
    }

    class IntConverter : VariableTypeConverter<Int> {
        override fun parse(value: String): Int {
            return value.toIntOrNull()
                    ?: throw FailureResponseException(400, "The value should be Int")
        }

        override fun stringify(value: Int): String {
            return value.toString()
        }
    }

    class LongConverter : VariableTypeConverter<Long> {
        override fun parse(value: String): Long {
            return value.toLongOrNull()
                    ?: throw FailureResponseException(400, "The value should be Long")
        }

        override fun stringify(value: Long): String {
            return value.toString()
        }
    }

    class FloatConverter : VariableTypeConverter<Float> {
        override fun parse(value: String): Float {
            return value.toFloatOrNull()
                    ?: throw FailureResponseException(400, "The value should be Float")
        }

        override fun stringify(value: Float): String {
            return value.toString()
        }
    }

    class DoubleConverter : VariableTypeConverter<Double> {
        override fun parse(value: String): Double {
            return value.toDoubleOrNull()
                    ?: throw FailureResponseException(400, "The value should be Double")
        }

        override fun stringify(value: Double): String {
            return value.toString()
        }
    }

    class BooleanConverter : VariableTypeConverter<Boolean> {
        override fun parse(value: String): Boolean {
            return when (value) {
                "true" -> true
                "false" -> false
                else -> throw FailureResponseException(400, "The value should be Int")
            }
        }

        override fun stringify(value: Boolean): String {
            return value.toString()
        }
    }
}