package com.github.niuhf0452.exile

import org.gradle.api.Plugin
import org.gradle.api.Project

class ExileKotlinSerializationPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        enableKotlinSerialization()
    }

    private fun Project.enableKotlinSerialization() {
        plugins.apply("org.jetbrains.kotlin.plugin.serialization")
        dependencies.run {
            add("implementation", Bom.`kotlinx-serialization`)
        }
    }
}
