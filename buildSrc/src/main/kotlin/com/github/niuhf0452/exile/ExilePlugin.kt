package com.github.niuhf0452.exile

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.plugins.signing.SigningExtension
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

open class ExileExtension {
    var kotlinTest = true
    var kotlinSerialization = false
    var kotlinAllOpen = false
    var jacoco = true
    var mavenPublish = false
}

class ExilePlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        val config = extensions.create("exile", ExileExtension::class.java)
        enableKotlin()
        if (config.kotlinSerialization) {
            enableKotlinSerialization()
        }
        if (config.kotlinAllOpen) {
            enableKotlinAllOpen()
        }
        if (config.kotlinTest) {
            enableKotlinTest()
        }
        if (config.jacoco) {
            enableJacoco()
        }
        if (config.mavenPublish) {
            enableMavenPublish()
        }
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

    private fun Project.enableKotlinSerialization() {
        plugins.apply("org.jetbrains.kotlin.plugin.serialization")
    }

    private fun Project.enableKotlinAllOpen() {
        plugins.apply("org.jetbrains.kotlin.plugin.allopen")
        extensions.getByType(AllOpenExtension::class.java).run {
            annotation("com.github.niuhf0452.exile.inject.Inject")
        }
    }

    private fun Project.enableKotlinTest() {
        tasks.withType(Test::class.java).configureEach {
            useJUnitPlatform()
        }

        dependencies.run {
            add("testImplementation", Bom.`kotlintest-runner-junit5`)
            add("testImplementation", Bom.`kotlinx-serialization`)
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

    private fun Project.enableMavenPublish() {
        plugins.run {
            apply("org.gradle.maven-publish")
            apply("org.gradle.signing")
        }
        extensions.configure(PublishingExtension::class.java) {
            publications {
                create("maven", MavenPublication::class.java) {
                    groupId = project.group.toString()
                    artifactId = project.name
                    version = project.version.toString()
                    from(components.getByName("java"))
                    versionMapping {
                        usage("java-api") {
                            fromResolutionOf("runtimeClasspath")
                        }
                        usage("java-runtime") {
                            fromResolutionResult()
                        }
                    }
                    pom {
                        name.set(project.name)
                        description.set("An Java framework for enterprise software.")
                        url.set("https://github.com/niuhf0452/exile")
                        licenses {
                            license {
                                name.set("The Apache License, Version 2.0")
                                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                            }
                        }
                        developers {
                            developer {
                                name.set("Haifeng Niu")
                                email.set("niuhf0452@gmail.com")
                            }
                        }
                        scm {
                            connection.set("scm:git:git://github.com/niuhf0452/exile.git")
                            developerConnection.set("scm:git:ssh://github.com/niuhf0452/exile.git")
                            url.set("https://github.com/niuhf0452/exile")
                        }
                    }
                }
            }
            repositories {
                maven {
                    credentials {
                        username = System.getProperty("sonatype.central.username")
                        password = System.getProperty("sonatype.central.password")
                    }
                    val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
                    val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots")
                    url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
                }
            }
        }

        extensions.configure(SigningExtension::class.java) {
            val publishing = extensions.getByType(PublishingExtension::class.java)
            sign(publishing.publications.getByName("maven"))
        }
    }
}
