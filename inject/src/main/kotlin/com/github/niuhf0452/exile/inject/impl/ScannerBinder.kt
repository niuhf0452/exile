package com.github.niuhf0452.exile.inject.impl

import com.github.niuhf0452.exile.inject.ClassScanner
import com.github.niuhf0452.exile.inject.Injector
import com.github.niuhf0452.exile.inject.TypeKey

class ScannerBinder(
        private val scanner: ClassScanner
) : Injector.Binder {
    override fun bind(key: TypeKey, context: Injector.BindingContext) {
        if (key.classifier == ClassScanner::class) {
            context.bindToInstance(emptyList(), scanner)
        }
    }
}