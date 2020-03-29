package com.github.niuhf0452.exile.web.impl

import com.github.niuhf0452.exile.web.*
import com.github.niuhf0452.exile.web.serialization.VariableTypeConverters
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions

class TypeSafeServerHandler(
        private val method: KFunction<*>,
        private val parameters: List<ParameterExtractor>
) : WebHandler {
    override suspend fun onRequest(context: RequestContext): WebResponse<Any> {
        val params = parameters.map { it.get(context) }
        val callByDefault = params.find { it === defaultValuePlaceHolder } != null
        val result = if (callByDefault) {
            val map = mutableMapOf<KParameter, Any?>()
            for (i in parameters.indices) {
                if (params[i] !== defaultValuePlaceHolder) {
                    map[parameters[i].parameter] = params[i]
                }
            }
            if (method.isSuspend) {
                method.callSuspendBy(map)
            } else {
                method.callBy(map)
            }
        } else {
            val args = params.toTypedArray()
            if (method.isSuspend) {
                method.callSuspend(*args)
            } else {
                method.call(*args)
            }
        }
        return when (result) {
            null, Unit -> WebResponse.newBuilder().statusCode(204).build()
            else -> WebResponse.newBuilder().statusCode(200).entity(result).build()
        }
    }

    interface ParameterExtractor {
        val parameter: KParameter

        fun get(context: RequestContext): Any?
    }

    private class InjectionParameterExtractor(
            override val parameter: KParameter,
            private val injector: TypeSafeHandlerInjector
    ) : ParameterExtractor {
        private val instanceClass = parameter.type.classifier as? KClass<*>
                ?: throw IllegalArgumentException("@WebEndpoint instance can't be created: $parameter")

        override fun get(context: RequestContext): Any? {
            return injector.getInstance(instanceClass)
        }
    }

    private abstract class ConvertibleExtractor(
            final override val parameter: KParameter
    ) : ParameterExtractor {
        private val isList: Boolean
        private val converter: VariableTypeConverter<*>
        private val name: String

        init {
            var c = parameter.type.classifier as? KClass<*>
                    ?: throw IllegalArgumentException("@WebMethod parameter type is not supported: $parameter")
            if (c == List::class) {
                isList = true
                c = parameter.type.arguments[0].type?.classifier as? KClass<*>
                        ?: throw IllegalArgumentException("@WebMethod parameter type is not supported: $parameter")
            } else {
                isList = false
            }
            converter = VariableTypeConverters.getConverter(c)
                    ?: throw IllegalArgumentException("@WebMethod parameter type is not supported: $parameter")
            @Suppress("LeakingThis")
            var value = getAnnotatedName()
            if (value == null || value.isEmpty()) {
                value = parameter.name
            }
            name = value ?: throw IllegalArgumentException("@WebMethod parameter name is missing: $parameter")
        }

        protected abstract fun getAnnotatedName(): String?

        protected abstract fun getValue(context: RequestContext, name: String): String?

        protected abstract fun getValueList(context: RequestContext, name: String): List<String>

        override fun get(context: RequestContext): Any? {
            if (isList) {
                val list = getValueList(context, name)
                if (list.isEmpty()) {
                    if (parameter.isOptional) {
                        return defaultValuePlaceHolder
                    }
                    return emptyList<Any>()
                }
                return list.map { text ->
                    converter.parse(text)
                }
            } else {
                val text = getValue(context, name)
                if (text == null) {
                    if (parameter.isOptional) {
                        return defaultValuePlaceHolder
                    }
                    throw FailureResponseException(400, "Parameter is missing: $name")
                }
                return converter.parse(text)
            }
        }
    }

    private class PathParamExtractor(parameter: KParameter) : ConvertibleExtractor(parameter) {
        override fun getAnnotatedName(): String? {
            return parameter.findAnnotation<WebQueryParam>()?.value
        }

        override fun getValue(context: RequestContext, name: String): String? {
            return context.pathParams[name]
                    ?: context.queryParams.get(name).firstOrNull()
        }

        override fun getValueList(context: RequestContext, name: String): List<String> {
            return context.queryParams.get(name).toList()
        }
    }

    private class QueryParamExtractor(parameter: KParameter) : ConvertibleExtractor(parameter) {
        override fun getAnnotatedName(): String? {
            return parameter.findAnnotation<WebQueryParam>()?.value
        }

        override fun getValue(context: RequestContext, name: String): String? {
            return context.pathParams[name]
                    ?: context.queryParams.get(name).firstOrNull()
        }

        override fun getValueList(context: RequestContext, name: String): List<String> {
            return context.queryParams.get(name).toList()
        }
    }

    private class HeaderExtractor(parameter: KParameter) : ConvertibleExtractor(parameter) {
        override fun getAnnotatedName(): String? {
            return parameter.findAnnotation<WebHeader>()?.value
        }

        override fun getValue(context: RequestContext, name: String): String? {
            return context.request.headers.get(name).firstOrNull()
        }

        override fun getValueList(context: RequestContext, name: String): List<String> {
            return context.request.headers.get(name).toList()
        }
    }

    private class EntityExtractor(
            override val parameter: KParameter
    ) : ParameterExtractor {
        private val cls = parameter.type.classifier as? KClass<*>
                ?: throw IllegalArgumentException("@WebMethod parameter type is unsupported: $parameter")

        override fun get(context: RequestContext): Any? {
            if (!context.request.hasEntity && parameter.isOptional) {
                return defaultValuePlaceHolder
            }
            return context.request.entity.convertTo(cls)
        }
    }

    companion object {
        private val defaultValuePlaceHolder = Any()

        fun addHandlers(router: Router, handlerClass: KClass<*>, injector: TypeSafeHandlerInjector) {
            val a = handlerClass.findAnnotation<WebEndpoint>()
                    ?: throw IllegalArgumentException("Type-safe web handler should be annotated " +
                            "with @WebEndpoint: $handlerClass")
            val basePath = a.value
            parseClass(router, handlerClass, basePath, injector)
        }

        private fun parseClass(router: Router, handlerClass: KClass<*>, basePath: String,
                               injector: TypeSafeHandlerInjector) {
            handlerClass.functions.forEach { m ->
                val a = m.findAnnotation<WebMethod>()
                if (a != null) {
                    if (m.visibility != KVisibility.PUBLIC) {
                        throw IllegalArgumentException("@WebMethod should be public: $m")
                    }
                    if (m.typeParameters.isNotEmpty()) {
                        throw IllegalArgumentException("@WebMethod should not have type parameter: $m")
                    }
                    parseMethod(router, m, a.method, Util.joinPath(basePath, a.path), injector)
                }
            }
        }

        private fun parseMethod(router: Router, handlerMethod: KFunction<*>, method: String, path: String,
                                injector: TypeSafeHandlerInjector) {
            val params = handlerMethod.parameters.map { p ->
                when {
                    p.kind == KParameter.Kind.INSTANCE -> InjectionParameterExtractor(p, injector)
                    p.kind == KParameter.Kind.EXTENSION_RECEIVER -> InjectionParameterExtractor(p, injector)
                    p.findAnnotation<WebPathParam>() != null -> PathParamExtractor(p)
                    p.findAnnotation<WebQueryParam>() != null -> QueryParamExtractor(p)
                    p.findAnnotation<WebHeader>() != null -> HeaderExtractor(p)
                    p.findAnnotation<WebEntity>() != null -> EntityExtractor(p)
                    else -> InjectionParameterExtractor(p, injector)
                }
            }
            try {
                router.addRoute(method, path, TypeSafeServerHandler(handlerMethod, params))
            } catch (ex: Exception) {
                throw IllegalArgumentException("Can't add @WebMethod to router: $handlerMethod", ex)
            }
        }
    }
}