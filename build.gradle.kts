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
        property("sonar.projectKey", "com.github.exile")
        property("sonar.organization", "niuhf0452")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
        csv.isEnabled = false
        html.destination = file("${buildDir}/jacocoHtml")
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.apiVersion = "1.3"
        kotlinOptions.languageVersion = "1.3"
    }

    dependencies {
        testImplementation("io.kotlintest:kotlintest-runner-junit5:${Vers.kotlin_test}")
    }
}