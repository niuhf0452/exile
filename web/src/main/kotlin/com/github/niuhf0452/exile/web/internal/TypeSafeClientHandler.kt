package com.github.niuhf0452.exile.web.internal

import com.github.niuhf0452.exile.common.AsyncMethodHandler
import com.github.niuhf0452.exile.common.ProxyFactory
import com.github.niuhf0452.exile.common.internal.JdkProxyFactoryBuilder
import com.github.niuhf0452.exile.web.*
import com.github.niuhf0452.exile.web.serialization.VariableTypeConverters
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation

object TypeSafeClientHandler {
    fun <A : Any> getClientFactory(cls: KClass<A>): TypeSafeClientFactory<A> {
        val basePath = cls.findAnnotation<WebEndpoint>()?.value ?: ""
        val proxyFactory = JdkProxyFactoryBuilder<State>()
                .addInterface(cls)
                .filter { it.findAnnotation<WebMethod>() != null }
                .handle { parseMethod(it, basePath) }
                .build()
        return Factory(proxyFactory)
    }

    private fun parseMethod(method: KFunction<*>, basePath: String): MethodHandler {
        val a = method.findAnnotation<WebMethod>()!!
        val param = mutableListOf<ParameterBuilder>()
        method.parameters.forEachIndexed { i, p ->
            val pb = parsePathParam(p, i - 1)
                    ?: parseQueryParam(p, i - 1)
                    ?: parseHeaderParam(p, i - 1)
                    ?: parseEntityParam(p, i - 1)
            if (pb != null) {
                param.add(pb)
            }
        }
        val returnClass = method.returnType.classifier as? KClass<*>
                ?: throw IllegalArgumentException("The return type is not supported: $method")
        return MethodHandler(method, a.method, Util.joinPath(basePath, a.path), param, returnClass)
    }

    private fun parsePathParam(parameter: KParameter, index: Int): ParameterBuilder? {
        val a = parameter.findAnnotation<WebPathParam>()
        if (a != null) {
            val name = if (a.value.isEmpty()) {
                parameter.name
                        ?: throw IllegalArgumentException("Can't find parameter name: $parameter")
            } else {
                a.value
            }
            return PathParamBuilder(index, name, getConverter(parameter))
        }
        return null
    }

    private fun parseQueryParam(parameter: KParameter, index: Int): ParameterBuilder? {
        val a = parameter.findAnnotation<WebQueryParam>()
        if (a != null) {
            val name = if (a.value.isEmpty()) {
                parameter.name
                        ?: throw IllegalArgumentException("Can't find parameter name: $parameter")
            } else {
                a.value
            }
            return QueryParamBuilder(index, name, getConverter(parameter))
        }
        return null
    }

    private fun parseHeaderParam(parameter: KParameter, index: Int): ParameterBuilder? {
        val a = parameter.findAnnotation<WebHeader>()
        if (a != null) {
            val name = if (a.value.isEmpty()) {
                parameter.name
                        ?: throw IllegalArgumentException("Can't find parameter name: $parameter")
            } else {
                a.value
            }
            return HeaderParamBuilder(index, name, getConverter(parameter))
        }
        return null
    }

    private fun parseEntityParam(parameter: KParameter, index: Int): ParameterBuilder? {
        if (parameter.findAnnotation<WebEntity>() != null) {
            return EntityParamBuilder(index)
        }
        return null
    }

    private fun getConverter(parameter: KParameter): VariableTypeConverter<Any> {
        var c = parameter.type.classifier as? KClass<*>
                ?: throw IllegalArgumentException("Parameter type is not supported: $parameter")
        if (c == List::class) {
            c = parameter.type.arguments[0].type?.classifier as? KClass<*>
                    ?: throw IllegalArgumentException("Parameter type is not supported: $parameter")
        }
        @Suppress("UNCHECKED_CAST")
        return VariableTypeConverters.getConverter(c) as? VariableTypeConverter<Any>
                ?: throw IllegalArgumentException("Can't find parameter converter: $parameter")
    }

    private class MethodHandler(
            override val function: KFunction<*>,
            private val method: String,
            private val path: String,
            private val parameters: List<ParameterBuilder>,
            private val returnClass: KClass<*>
    ) : AsyncMethodHandler<State>() {
        override suspend fun asyncCall(state: State, instance: Any, args: Array<out Any?>?): Any? {
            val (client, uri) = state
            val builder = WebRequest.newBuilder(method, Util.joinPath(uri, path))
            if (args != null) {
                parameters.forEach { p ->
                    p.build(builder, args[p.index])
                }
            }
            @Suppress("UNCHECKED_CAST")
            val request = builder.build() as WebRequest<Any>
            val response = client.send(request)
            if (response.statusCode < 200 || response.statusCode > 299) {
                throw ClientResponseException(response,
                        "Response with failure status code: ${response.statusCode}, $method $path")
            }
            val entity = response.entity
            if (entity == null) {
                if (!function.returnType.isMarkedNullable) {
                    throw ClientResponseException(response,
                            "Response with no entity, but expect entity: ${response.statusCode}, $method $path")
                }
                return null
            }
            return entity.convertTo(returnClass)
        }
    }

    interface ParameterBuilder {
        val index: Int

        fun build(request: WebRequest.Builder<*>, value: Any?)
    }

    class PathParamBuilder(
            override val index: Int,
            private val name: String,
            private val converter: VariableTypeConverter<Any>
    ) : ParameterBuilder {
        override fun build(request: WebRequest.Builder<*>, value: Any?) {
            if (value == null) {
                throw IllegalArgumentException("Path parameter can be null: $name")
            }
            val stringValue = converter.stringify(value)
            request.setPathParam(name, stringValue)
        }
    }

    class QueryParamBuilder(
            override val index: Int,
            private val name: String,
            private val converter: VariableTypeConverter<Any>
    ) : ParameterBuilder {
        override fun build(request: WebRequest.Builder<*>, value: Any?) {
            if (value != null) {
                if (value is List<*>) {
                    value.forEach { v ->
                        if (v != null) {
                            request.addQueryParam(name, converter.stringify(v))
                        }
                    }
                } else {
                    request.addQueryParam(name, converter.stringify(value))
                }
            }
        }
    }

    class HeaderParamBuilder(
            override val index: Int,
            private val name: String,
            private val converter: VariableTypeConverter<Any>
    ) : ParameterBuilder {
        override fun build(request: WebRequest.Builder<*>, value: Any?) {
            if (value != null) {
                if (value is List<*>) {
                    value.forEach { v ->
                        if (v != null) {
                            request.addHeader(name, converter.stringify(v))
                        }
                    }
                } else {
                    request.addHeader(name, converter.stringify(value))
                }
            }
        }
    }

    class EntityParamBuilder(
            override val index: Int
    ) : ParameterBuilder {
        override fun build(request: WebRequest.Builder<*>, value: Any?) {
            request.entity(value)
        }
    }

    private data class State(val client: WebClient, val uri: String)

    private class Factory<A>(
            private val proxyFactory: ProxyFactory<State>
    ) : TypeSafeClientFactory<A> {
        override fun getClient(client: WebClient, uri: String): A {
            @Suppress("UNCHECKED_CAST")
            return proxyFactory.createObject(State(client, uri)) as A
        }
    }
}