package com.github.niuhf0452.exile

import com.github.jk1.license.LicenseReportExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class ExileKotlinPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        enableKotlin()
        enableLicense()
    }

    private fun Project.enableKotlin() {
        plugins.run {
            apply("org.gradle.java-library")
            apply("org.jetbrains.kotlin.jvm")
        }

        group = rootProject.group
        version = rootProject.version

        repositories.run {
            mavenCentral()
            jcenter()
        }

        extensions.getByType(JavaPluginExtension::class.java).run {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
            withJavadocJar()
            withSourcesJar()
        }

        tasks.getByName("javadoc").run {
            this as Javadoc
            if (JavaVersion.current().isJava9Compatible) {
                (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
            }
        }

        tasks.withType(KotlinCompile::class.java).configureEach {
            kotlinOptions.jvmTarget = "11"
            kotlinOptions.apiVersion = "1.3"
            kotlinOptions.languageVersion = "1.3"
        }
    }

    private fun Project.enableLicense() {
        plugins.run {
            apply("com.github.jk1.dependency-license-report")
        }

        extensions.configure(LicenseReportExtension::class.java) {
            allowedLicensesFile = rootProject.file("allowed-licenses.json")
        }
    }
}
