package com.github.niuhf0452.exile

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension

class ExileKotlinAllOpenPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        enableKotlinAllOpen()
    }

    private fun Project.enableKotlinAllOpen() {
        plugins.apply("org.jetbrains.kotlin.plugin.allopen")

        extensions.getByType(AllOpenExtension::class.java).run {
            annotation("com.github.niuhf0452.exile.inject.Inject")
            annotation("com.github.niuhf0452.exile.inject.Factory")
        }
    }
}
