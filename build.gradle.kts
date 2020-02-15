import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    idea
    kotlin("jvm") version Vers.kotlin
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