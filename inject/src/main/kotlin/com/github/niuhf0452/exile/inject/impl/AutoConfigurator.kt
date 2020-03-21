package com.github.niuhf0452.exile.inject.impl

import com.github.niuhf0452.exile.inject.InjectorAutoLoader
import com.github.niuhf0452.exile.inject.InjectorBuilder
import com.github.niuhf0452.exile.inject.enhancer.ByteBuddyEnhancer
import java.util.*

class AutoConfigurator(
        private val packageNames: List<String>,
        private val builder: InjectorBuilder
) {
    fun configure(): InjectorBuilder {
        packageNames.forEach { packageName ->
            builder.addPackage(packageName)
        }
        builder.scanner(ClassgraphScanner.Factory())
        builder.enhancer(ByteBuddyEnhancer())
        ServiceLoader.load(InjectorAutoLoader::class.java).forEach { loader ->
            loader.getBinders().forEach { binder ->
                builder.addBinder(binder)
            }
            loader.getFilters().forEach { filter ->
                builder.addFilter(filter)
            }
        }
        return builder
    }
}