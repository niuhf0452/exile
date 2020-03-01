package io.github.niuhf0452.exile.inject.impl

import io.github.niuhf0452.exile.inject.Injector
import io.github.niuhf0452.exile.inject.Scope
import io.github.niuhf0452.exile.inject.ScopeQualifier
import io.github.niuhf0452.exile.inject.TypeKey
import java.security.MessageDigest
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

class ScopeSupportFilter : Injector.Filter {
    override val order: Int
        get() = Int.MAX_VALUE

    override fun filter(binding: Injector.Binding): Injector.Binding {
        binding.qualifiers.forEach { q ->
            val sq = q.annotationClass.findAnnotation<ScopeQualifier>()
            if (sq != null) {
                val key = generateKey(binding)
                val scope = createScope(sq.value)
                return ScopedBinding(key, q, scope, binding)
            }
        }
        return binding
    }

    @Suppress("UNCHECKED_CAST")
    private fun createScope(cls: KClass<*>): Scope<Annotation> {
        val context = InjectorImpl.contextHolder.get()
        val scope = context.getBindings(TypeKey(cls)).getSingle().getInstance()
        return scope as Scope<Annotation>
    }

    private fun generateKey(binding: Injector.Binding): String {
        val sb = StringBuilder()
        binding.qualifiers.joinTo(sb, separator = "\n", postfix = "\n")
        sb.append(binding.key)
        val md = MessageDigest.getInstance("SHA1")
        val buf = md.digest(sb.toString().toByteArray())
        val hash = toHex(buf)
        val name = binding.key.classifier.simpleName
                ?: throw IllegalStateException("Can't cache an object without simpleName: ${binding.key}")
        return "$name@$hash"
    }

    private fun toHex(buf: ByteArray): String {
        val sb = StringBuilder()
        buf.forEach { b ->
            val i = b.toInt()
            sb.append('0' + ((i shr 4) and 0x0f))
            sb.append('0' + (i and 0x0f))
        }
        return sb.toString()
    }

    private class ScopedBinding<A : Annotation>(
            private val cacheKey: String,
            private val qualifier: A,
            private val scope: Scope<A>,
            private val binding: Injector.Binding
    ) : Injector.Binding {
        override val key: TypeKey
            get() = binding.key
        override val qualifiers: List<Annotation>
            get() = binding.qualifiers

        override fun getInstance(): Any {
            return scope.getContainer().getOrCreate(cacheKey, qualifier) {
                binding.getInstance()
            }
        }

        override fun toString(): String {
            return "Scoped(key = $key, qualifier = $qualifier, scope = ${scope::class}, binding = $binding)"
        }
    }
}