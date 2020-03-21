package com.github.niuhf0452.exile.config.impl

import com.github.niuhf0452.exile.config.Config
import com.github.niuhf0452.exile.config.ConfigFragment
import com.github.niuhf0452.exile.config.ConfigValue
import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.internal.TaggedDecoder
import kotlinx.serialization.internal.TaggedEncoder
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.SerialModule
import java.util.*

class SimpleConfig(
        override val context: SerialModule = EmptyModule
) : SerialFormat {
    fun <T> parse(conf: ConfigFragment, serial: DeserializationStrategy<T>): T {
        return ClassDecoder(conf, "").decode(serial)
    }

    fun <T> toConfig(data: T, serial: SerializationStrategy<T>): ConfigFragment {
        val map = TreeMap<String, ConfigValue>()
        val encoder = ClassEncoder(map, EmptyConfig.EmptySource, "")
        encoder.encode(serial, data)
        return ConfigFragmentImpl(map)
    }

    @OptIn(InternalSerializationApi::class)
    private inner class ClassEncoder(
            private val map: MutableMap<String, ConfigValue>,
            private val source: Config.Source,
            private val prefix: String
    ) : TaggedEncoder<String>() {
        override val context: SerialModule
            get() = this@SimpleConfig.context

        override fun SerialDescriptor.getTag(index: Int): String {
            return getElementName(index)
        }

        override fun encodeTaggedValue(tag: String, value: Any) {
            val path = if (prefix.isEmpty()) tag else "$prefix.$tag"
            map[path] = ConfigValue(source, path, value.toString())
        }

        override fun beginStructure(descriptor: SerialDescriptor, vararg typeSerializers: KSerializer<*>): CompositeEncoder {
            val tag = currentTagOrNull
            val path = when {
                tag == null -> prefix
                prefix.isEmpty() -> tag
                else -> "$prefix.$tag"
            }
            return when (descriptor.kind) {
                StructureKind.CLASS, StructureKind.OBJECT -> ClassEncoder(map, source, path)
                StructureKind.LIST -> ListEncoder(map, source, path)
                StructureKind.MAP -> MapEncoder(map, source, path)
                else -> throw IllegalStateException()
            }
        }
    }

    @OptIn(InternalSerializationApi::class)
    private inner class ListEncoder(
            private val map: MutableMap<String, ConfigValue>,
            private val source: Config.Source,
            private val prefix: String
    ) : TaggedEncoder<Int>() {
        override fun SerialDescriptor.getTag(index: Int): Int {
            return index
        }

        override fun encodeTaggedValue(tag: Int, value: Any) {
            val path = if (prefix.isEmpty()) tag.toString() else "$prefix.$tag"
            map[path] = ConfigValue(source, path, value.toString())
        }

        override fun beginStructure(descriptor: SerialDescriptor, vararg typeSerializers: KSerializer<*>): CompositeEncoder {
            val path = if (prefix.isEmpty()) currentTag.toString() else "$prefix.$currentTag"
            return when (descriptor.kind) {
                StructureKind.CLASS, StructureKind.OBJECT -> ClassEncoder(map, source, path)
                StructureKind.LIST -> ListEncoder(map, source, path)
                StructureKind.MAP -> MapEncoder(map, source, path)
                else -> throw IllegalStateException()
            }
        }
    }

    @OptIn(InternalSerializationApi::class)
    private inner class MapEncoder(
            private val map: MutableMap<String, ConfigValue>,
            private val source: Config.Source,
            private val prefix: String
    ) : TaggedEncoder<Int>() {
        private var key: String = ""

        override fun SerialDescriptor.getTag(index: Int): Int {
            return index
        }

        override fun encodeTaggedValue(tag: Int, value: Any) {
            if (tag % 2 == 0) {
                key = value.toString()
            } else {
                val path = if (prefix.isEmpty()) key else "$prefix.$key"
                map[path] = ConfigValue(source, path, value.toString())
            }
        }

        override fun beginStructure(descriptor: SerialDescriptor, vararg typeSerializers: KSerializer<*>): CompositeEncoder {
            if (currentTag % 2 == 0) {
                throw IllegalArgumentException("The key of map must be primitive type")
            }
            val path = if (prefix.isEmpty()) key else "$prefix.$key"
            return when (descriptor.kind) {
                StructureKind.CLASS, StructureKind.OBJECT -> ClassEncoder(map, source, path)
                StructureKind.LIST -> ListEncoder(map, source, path)
                StructureKind.MAP -> MapEncoder(map, source, path)
                else -> throw IllegalStateException()
            }
        }
    }

    @OptIn(InternalSerializationApi::class)
    private abstract inner class AbstractDecoder<T> : TaggedDecoder<T>() {
        override val context: SerialModule
            get() = this@SimpleConfig.context

        protected abstract fun getValue(tag: T): ConfigValue

        override fun decodeTaggedString(tag: T) = getValue(tag).asString()
        override fun decodeTaggedByte(tag: T): Byte = getValue(tag).asInt().toByte()
        override fun decodeTaggedShort(tag: T): Short = getValue(tag).asInt().toShort()
        override fun decodeTaggedInt(tag: T): Int = getValue(tag).asInt()
        override fun decodeTaggedLong(tag: T): Long = getValue(tag).asLong()
        override fun decodeTaggedFloat(tag: T): Float = getValue(tag).asDouble().toFloat()
        override fun decodeTaggedDouble(tag: T): Double = getValue(tag).asDouble()
        override fun decodeTaggedBoolean(tag: T): Boolean = getValue(tag).asBoolean()

        override fun decodeTaggedChar(tag: T): Char {
            val s = getValue(tag).asString()
            if (s.length != 1) throw SerializationException("String \"$s\" is not convertible to Char")
            return s[0]
        }

        override fun decodeTaggedValue(tag: T): Any = getValue(tag).value

        override fun decodeTaggedNotNullMark(tag: T) = true

        override fun decodeTaggedEnum(tag: T, enumDescription: SerialDescriptor): Int {
            val s = getValue(tag).asString()
            return enumDescription.getElementIndexOrThrow(s)
        }
    }

    private inner class ClassDecoder(
            private val config: ConfigFragment,
            private val prefix: String
    ) : AbstractDecoder<String>() {
        private var index = 0

        override fun SerialDescriptor.getTag(index: Int): String {
            return getElementName(index)
        }

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            if (index >= descriptor.elementsCount) {
                return READ_DONE
            }
            return index++
        }

        override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
            val tag = currentTagOrNull
            val path = when {
                tag == null -> prefix
                prefix.isEmpty() -> tag
                else -> "$prefix.$tag"
            }
            return when (descriptor.kind) {
                StructureKind.CLASS, StructureKind.OBJECT -> ClassDecoder(config, path)
                StructureKind.LIST -> ListDecoder(config, path)
                StructureKind.MAP -> MapDecoder(config, path)
                else -> throw IllegalStateException()
            }
        }

        override fun getValue(tag: String): ConfigValue {
            val path = if (prefix.isEmpty()) tag else "$prefix.$tag"
            return config.get(path)
        }
    }

    private inner class ListDecoder(
            private val config: ConfigFragment,
            private val prefix: String
    ) : AbstractDecoder<Int>() {
        private var index = 0

        override fun SerialDescriptor.getTag(index: Int): Int {
            return index
        }

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            if (config.find("$prefix.$index") != null) {
                return index++
            }
            val fragment = config.getFragment("$prefix.$index", keepPrefix = true)
            if (fragment.iterator().hasNext()) {
                return index++
            }
            return READ_DONE
        }

        override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
            val path = if (prefix.isEmpty()) currentTag.toString() else "$prefix.$currentTag"
            return when (descriptor.kind) {
                StructureKind.CLASS, StructureKind.OBJECT -> ClassDecoder(config, path)
                StructureKind.LIST -> ListDecoder(config, path)
                StructureKind.MAP -> MapDecoder(config, path)
                else -> throw IllegalStateException()
            }
        }

        override fun getValue(tag: Int): ConfigValue {
            val path = if (prefix.isEmpty()) tag.toString() else "$prefix.$tag"
            return config.get(path)
        }
    }

    private inner class MapDecoder(
            private val config: ConfigFragment,
            private val prefix: String
    ) : AbstractDecoder<Int>() {
        private var index = 0
        private val keys = config.getFragment(prefix, keepPrefix = true)
                .mapTo(mutableSetOf()) { v ->
                    val start = if (prefix.isEmpty()) 0 else prefix.length + 1
                    var end = v.path.indexOf('.', start)
                    if (end < 0) {
                        end = v.path.length
                    }
                    v.path.substring(start, end)
                }
                .toList()

        override fun SerialDescriptor.getTag(index: Int): Int {
            return index
        }

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            if (index / 2 < keys.size) {
                return index++
            }
            return READ_DONE
        }

        override fun getValue(tag: Int): ConfigValue {
            val key = keys[tag / 2]
            if (tag % 2 == 0) {
                return ConfigValue(EmptyConfig.EmptySource, key, key)
            }
            val path = if (prefix.isEmpty()) key else "$prefix.$key"
            return config.get(path)
        }

        override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
            if (currentTag % 2 == 0) {
                throw IllegalArgumentException("The key of map must be primitive type")
            }
            val key = keys[currentTag / 2]
            val path = if (prefix.isEmpty()) key else "$prefix.$key"
            return when (descriptor.kind) {
                StructureKind.CLASS, StructureKind.OBJECT -> ClassDecoder(config, path)
                StructureKind.LIST -> ListDecoder(config, path)
                StructureKind.MAP -> MapDecoder(config, path)
                else -> throw IllegalStateException()
            }
        }
    }
}