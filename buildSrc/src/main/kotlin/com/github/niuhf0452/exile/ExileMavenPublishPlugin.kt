package com.github.niuhf0452.exile

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension

class ExileMavenPublishPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        enableMavenPublish()
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
