package io.github.niuhf0452.exile.inject.impl

import io.github.niuhf0452.exile.inject.ClassScanner
import io.github.niuhf0452.exile.inject.Injector
import io.github.niuhf0452.exile.inject.TypeKey

class ScannerBinder(
        private val scanner: ClassScanner
) : Injector.Binder {
    override fun bind(key: TypeKey, context: Injector.BindingContext) {
        if (key.classifier == ClassScanner::class) {
            context.bindToInstance(emptyList(), scanner)
        }
    }
}