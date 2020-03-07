import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    jacoco
    kotlin("jvm") version Vers.kotlin
    id("org.sonarqube") version "2.8"
}

repositories {
    mavenCentral()
}

group = "com.github.niuhf0452"
version = "0.1-SNAPSHOT"

idea {
    project {
        jdkName = "11"
        languageLevel = IdeaLanguageLevel("11")
    }
    module {
        isDownloadSources = true
    }
}

sonarqube {
    properties {
        property("sonar.projectName", "exile")
        property("sonar.projectKey", "com.github.niuhf0452.exile")
        property("sonar.organization", "niuhf0452")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "jacoco")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    jacoco {
        toolVersion = "0.8.5"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.apiVersion = "1.3"
        kotlinOptions.languageVersion = "1.3"
    }

    tasks.jacocoTestReport {
        reports {
            xml.isEnabled = true
        }
    }

    dependencies {
        testImplementation("io.kotlintest:kotlintest-runner-junit5:${Vers.kotlin_test}")
    }
}