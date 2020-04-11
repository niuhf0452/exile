package com.github.niuhf0452.exile.core

import com.github.niuhf0452.exile.common.PublicApi
import com.github.niuhf0452.exile.core.ErrorCode.Register
import kotlinx.serialization.*
import java.util.*
import kotlin.reflect.jvm.jvmName

/**
 * A status code standard for logging and responding.
 *
 * For each log and server response, a status code is shipped to identity the meaning of log or response.
 *
 * The status code is consist by a short module name and a sequence number. All status codes should be predefine
 * with certain meanings when the application is designed.
 *
 * The framework collect all the codes using [Register].
 *
 * @since 1.0
 */
@PublicApi
@Serializable(with = ErrorCode.Serializer::class)
data class ErrorCode(val module: String, val code: Int) {
    override fun toString(): String {
        return "$module-$code"
    }

    companion object {
        /**
         * OK is the only ErrorCode not stand for error.
         */
        val OK = ErrorCode("OK", 200)

        private val pattern = "([A-Z0-9]+)-(\\d+)".toRegex()

        fun parse(value: String): ErrorCode {
            val m = pattern.matchEntire(value)
                    ?: throw IllegalArgumentException("Invalid status code format")
            return ErrorCode(m.groupValues[1], m.groupValues[2].toInt())
        }

        val allErrorCodes: List<ErrorCode> by lazy {
            val codes = mutableListOf<ErrorCode>()
            ServiceLoader.load(Register::class.java).forEach { reg ->
                codes.addAll(reg.getCodes())
            }
            codes
        }
    }

    class Serializer : KSerializer<ErrorCode> {
        override val descriptor: SerialDescriptor = PrimitiveDescriptor(ErrorCode::class.jvmName, PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): ErrorCode {
            return parse(decoder.decodeString())
        }

        override fun serialize(encoder: Encoder, value: ErrorCode) {
            encoder.encodeString(value.toString())
        }
    }

    interface Register {
        fun getCodes(): List<ErrorCode>
    }
}
