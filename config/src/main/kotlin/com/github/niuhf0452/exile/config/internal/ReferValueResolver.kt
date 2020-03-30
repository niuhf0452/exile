package com.github.niuhf0452.exile.config.internal

import com.github.niuhf0452.exile.config.Config
import com.github.niuhf0452.exile.config.internal.Util.configPathRegex

class ReferValueResolver : Config.ValueResolver {
    override fun resolve(value: String, context: Config.ValueResolverContext): String? {
        if (configPathRegex.matches(value)) {
            return context.find(value)
        }
        return null
    }
}