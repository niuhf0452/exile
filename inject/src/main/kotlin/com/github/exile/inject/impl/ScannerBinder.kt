package com.github.exile.inject.impl

import com.github.exile.inject.Injector
import com.github.exile.inject.TypeKey

class ScannerBinder(
        private val scanner: Injector.Scanner
) : Injector.Binder {
    override fun bind(key: TypeKey, context: Injector.BindingContext) {
        if (key.classifier == Injector.Scanner::class) {
            context.bindToInstance(emptyList(), scanner)
        }
    }
}