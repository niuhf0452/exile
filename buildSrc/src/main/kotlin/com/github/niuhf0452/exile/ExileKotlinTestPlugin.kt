package com.github.niuhf0452.exile

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

class ExileKotlinTestPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        enableKotlinTest()
        enableJacoco()
    }

    private fun Project.enableKotlinTest() {
        tasks.withType(Test::class.java).configureEach {
            useJUnitPlatform()
        }

        dependencies.run {
            add("testImplementation", Bom.`kotlintest-runner-junit5`)
        }
    }

    private fun Project.enableJacoco() {
        plugins.run {
            apply("org.gradle.jacoco")
        }

        extensions.configure(JacocoPluginExtension::class.java) {
            toolVersion = "0.8.5"
        }

        tasks.getByName("jacocoTestReport").run {
            this as JacocoReport
            reports {
                xml.isEnabled = true
            }
        }
    }
}
