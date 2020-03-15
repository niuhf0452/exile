package com.github.niuhf0452.exile.config.impl

import com.github.niuhf0452.exile.config.Config
import java.util.concurrent.CopyOnWriteArrayList

class CompositeValueResolver : Config.ValueResolver {
    private val resolvers = CopyOnWriteArrayList<Config.ValueResolver>()

    override fun resolve(value: String, context: Config.ValueResolverContext): String? {
        resolvers.forEach { r ->
            val resolved = r.resolve(value, context)
            if (resolved != null) {
                return resolved
            }
        }
        return null
    }

    fun addResolver(resolver: Config.ValueResolver) {
        resolvers.add(resolver)
    }
}